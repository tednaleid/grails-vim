#!/usr/bin/env groovy


// errors and failures are both structured similarly in
// the xml report.  Generalize both as a 'problem'
def processProblem(problem) {
    def className = problem.parent().attribute('classname') 

    // remove package name from test.
    def splitted = className.toString().split("\\.")
    if (splitted.size()) {
        className = splitted[-1]
    }

    // Get line number by finding (testName.groovy:lineNumber)
    def matcher = problem.text() =~ /\((${className}.*)\)/
    // If nothing's found in the text(), then just use className + '.groovy', plus a zero-line number
    def fileNameWithErrorLine = matcher.find() ? matcher[0][1] : className + '.groovy:0'


    // Get problem message.  
    // Prepend the name of the test to it.
    def msg = " : " + problem.parent().attribute('name') + " : "
    // Sometimes the message is blank, so use 'type' attribute.
    msg += problem.attribute('message') ?: problem.attribute('type') ?: ''

    // Find this file in the dir. structure so we can get abs. path
    def fullPath
    new File("test").eachFileRecurse { f ->
        if (f.name == className + '.groovy') {
            fullPath = f.parent + '/' + fileNameWithErrorLine
            return
        }
    }
    println "${fullPath}:${msg}"
}

def testSuiteFile = new File('target/test-reports/TESTS-TestSuites.xml')
def failedTestCount = 0

if (testSuiteFile.exists()){
    def ts = new XmlParser().parse(testSuiteFile)

    // Process two things: failures and errors

    ts.testsuite.testcase.failure.each { failure ->
        failedTestCount++
        processProblem(failure)
    }

    ts.testsuite.testcase.error.each { err ->
        failedTestCount++
        processProblem(err)
    }
}
// used by vim to determine whether to display the quickfix buffer, 0 means no failed tests
System.exit(failedTestCount)

