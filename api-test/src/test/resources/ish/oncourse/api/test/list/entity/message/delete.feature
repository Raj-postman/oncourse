@parallel=false
Feature: Main feature for all DELETE requests with path 'list/entity/message'

    Background: Authorize first
        * call read('../../../signIn.feature')
        * url 'https://127.0.0.1:8182/a/v1'
        * def ishPath = 'list/entity/message'
        * def ishPathLogin = 'login'
        * def ishPathPlain = 'list/plain'
        * configure httpClientClass = 'ish.oncourse.api.test.client.KarateClient'


        
    Scenario: (+) Delete existing Message by admin

        Given path ishPath + '/1005'
        When method DELETE
        Then status 204

#       <---> Verification of deleting
        Given path ishPath + '/1005'
        When method GET
        Then status 400
        And match $.errorMessage == "Record with id = '1005' doesn't exist."



    Scenario: (+) Delete existing Message by notadmin

#       <--->  Login as notadmin
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsHide', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"
#       <--->

        Given path ishPath + '/1004'
        When method DELETE
        Then status 204

#       <---> Verification of deleting
        Given path ishPath + '/1004'
        When method GET
        Then status 400
        And match $.errorMessage == "Record with id = '1004' doesn't exist."



    Scenario: (-) Delete NOT existing Message

        Given path ishPath + '/99999'
        When method DELETE
        Then status 400
        And match response.errorMessage == "Record with id = '99999' doesn't exist."
