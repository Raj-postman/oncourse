@parallel=false
Feature: Main feature for all GET requests with path 'list/entity/application'

    Background: Authorize first
        * callonce read('../../../signIn.feature')
        * url 'https://127.0.0.1:8182/a/v1'
        * def ishPath = 'list/entity/application'
        * def ishPathLogin = 'login'
        * def ishPathList = 'list'
        * configure httpClientClass = 'ish.oncourse.api.test.client.KarateClient'



    Scenario: (+) Get list of all Applications by admin

        Given path ishPathList
        And param entity = 'Application'
        When method GET
        Then status 200
        And match $.rows[*].id contains ["1000"]



    Scenario: (+) Get Application by admin

        Given path ishPath + '/1000'
        When method GET
        Then status 200
        And match $ ==
        """
        {
        "id":1000,
        "contactId":4,
        "studentName":"stud3",
        "courseId":3,
        "courseName":"Course3 course3",
        "applicationDate":"#ignore",
        "status":"New",
        "source":"office",
        "feeOverride":33.00,
        "enrolBy":"2030-05-31",
        "createdBy":"onCourse Administrator",
        "reason":"Some reason",
        "documents":[],
        "tags":[],
        "customFields":{},
        "createdOn":"#ignore",
        "modifiedOn":"#ignore"
        }
        """


    Scenario: (-) Get not existing Application

        Given path ishPath + "/9999"
        When method GET
        Then status 400
        And match $.errorMessage == "Record with id = '9999' doesn't exist."



    Scenario: (-) Get Application without id in path

        Given path ishPath
        When method GET
        Then status 405


    Scenario: (+) Get list of all Applications by notadmin with access rights

        Given path '/logout'
        And request {}
        When method PUT
        
#       <--->  Login as notadmin
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsView', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path '/login'
        And request loginBody
        When method PUT
        Then status 200
#       <--->

        Given path ishPathList
        And param entity = 'Application'
        When method GET
        Then status 200
        And match $.rows[*].id contains ["1000"]



    Scenario: (+) Get Application by notadmin with access rights

        Given path '/logout'
        And request {}
        When method PUT
        
#       <--->  Login as notadmin
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsView', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path '/login'
        And request loginBody
        When method PUT
        Then status 200
#       <--->

        Given path ishPath + '/1000'
        When method GET
        Then status 200
        And match $ ==
        """
        {
        "id":1000,
        "contactId":4,
        "studentName":"stud3",
        "courseId":3,
        "courseName":"Course3 course3",
        "applicationDate":"#ignore",
        "status":"New",
        "source":"office",
        "feeOverride":33.00,
        "enrolBy":"2030-05-31",
        "createdBy":"onCourse Administrator",
        "reason":"Some reason",
        "documents":[],
        "tags":[],
        "customFields":{},
        "createdOn":"#ignore",
        "modifiedOn":"#ignore"
        }
        """



    Scenario: (-) Get list of all Applications by notadmin without access rights

        Given path '/logout'
        And request {}
        When method PUT
        
#       <--->  Login as notadmin
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsHide', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path '/login'
        And request loginBody
        When method PUT
        Then status 200
#       <--->

        Given path ishPathList
        And param entity = 'Application'
        When method GET
        Then status 403
        And match $.errorMessage == "Sorry, you have no permissions to view this entity. Please contact your administrator"



    Scenario: (-) Get Application by notadmin without access rights

        Given path '/logout'
        And request {}
        When method PUT
        
#       <--->  Login as notadmin
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsHide', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path '/login'
        And request loginBody
        When method PUT
        Then status 200
#       <--->

        Given path ishPath + "/1000"
        When method GET
        Then status 403
        And match $.errorMessage == "Sorry, you have no permissions to get application. Please contact your administrator"
