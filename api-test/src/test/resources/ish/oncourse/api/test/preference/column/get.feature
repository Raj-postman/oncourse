@parallel=false
Feature: Main feature for all GET requests with path 'preference/column'

    Background: Authorize first
        * callonce read('../../signIn.feature')
        * url 'https://127.0.0.1:8182/a/v1'
        * def ishPath = 'preference/column'
        * configure httpClientClass = 'ish.oncourse.api.test.client.KarateClient'
        

    Scenario: (+) Get column view settings

        Given path ishPath
        When method GET
        Then status 200
        And match response == {"preferenceLeftColumnWidth":200,"tagLeftColumnWidth":200,"securityLeftColumnWidth":200,"automationLeftColumnWidth":250}