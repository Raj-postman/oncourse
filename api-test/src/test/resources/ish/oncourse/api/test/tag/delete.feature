@parallel=false
Feature: Main feature for all DELETE requests with path 'tag'

    Background: Authorize first
        * call read('../signIn.feature')
        * url 'https://127.0.0.1:8182/a/v1'
        * def ishPath = 'tag'
        * def ishPathLogin = 'login'
        * configure httpClientClass = 'ish.oncourse.api.test.client.KarateClient'
        
        
    Scenario: (+) Delete not system tag group by admin

#       <----->  Add a new tag group for deleting:
        * def newTagGroup = {"name":"tagName001","status":"Private","system":false,"urlPath":null,"content":null,"weight":1,"requirements":[{"type":"Assessment","mandatory":false,"limitToOneTag":true,"system":false}],"childTags":[]}

        Given path ishPath
        And request newTagGroup
        When method POST
        Then status 204
#       <----->

        Given path ishPath
        When method GET
        Then status 200
        * def id = get[0] response[?(@.name == 'tagName001')].id

        Given path ishPath + '/' + id
        When method DELETE
        Then status 204


    Scenario: (+) Delete not system tag group by notadmin with permissions: Hide-View-Print-Edit-Create-Delete

#       <--->  Login as notadmin with max permissions and create new tag group for deleting:
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsDelete', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

        * def newTagGroup = {"name":"tagName600","status":"Private","system":false,"urlPath":null,"content":null,"weight":1,"requirements":[{"type":"Assessment","mandatory":false,"limitToOneTag":true,"system":false}],"childTags":[]}

        Given path ishPath
        And request newTagGroup
        When method POST
        Then status 204

#       Delete created tag group by notadmin:
        Given path ishPath
        When method GET
        Then status 200
        * def id = get[0] response[?(@.name == 'tagName600')].id

        Given path ishPath + '/' + id
        When method DELETE
        Then status 204

#       >>> Assertion:
        Given path ishPath
        When method GET
        Then status 200
        And match response[*].name !contains "tagName600"



    Scenario: (-) Delete not system tag group by notadmin with permissions: Hide-View-Print-Edit-Create

#       <--->  Login as admin and create new tag group for deleting:
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'admin', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

        * def newTagGroup = {"name":"tagName601","status":"Private","system":false,"urlPath":null,"content":null,"weight":1,"requirements":[{"type":"Assessment","mandatory":false,"limitToOneTag":true,"system":false}],"childTags":[]}

        Given path ishPath
        And request newTagGroup
        When method POST
        Then status 204

#       <--->  Login as notadmin:
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsCreate', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

#       >>> Delete created tag group by notadmin with permissions: Hide-View-Print-Edit-Create:
        Given path ishPath
        When method GET
        Then status 200
        * def id = get[0] response[?(@.name == 'tagName601')].id

        Given path ishPath + '/' + id
        When method DELETE
        Then status 403

#       <---->  Scenario have been finished. Now find and remove created object from DB:
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'admin', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

        Given path ishPath + '/' + id
        When method DELETE
        Then status 204
#       <--->



    Scenario: (-) Delete not system tag group by notadmin with permissions: Hide-View-Print-Edit

#       <--->  Create new tag group for deleting:
        * def newTagGroup = {"name":"tagName602","status":"Private","system":false,"urlPath":null,"content":null,"weight":1,"requirements":[{"type":"Assessment","mandatory":false,"limitToOneTag":true,"system":false}],"childTags":[]}

        Given path ishPath
        And request newTagGroup
        When method POST
        Then status 204

#       <--->  Login as notadmin:
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsEdit', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

#       >>> Delete created tag group by notadmin with permissions Hide-View-Print-Edit:
        Given path ishPath
        When method GET
        Then status 200
        * def id = get[0] response[?(@.name == 'tagName602')].id

        Given path ishPath + '/' + id
        When method DELETE
        Then status 403

#       <---->  Scenario have been finished. Now change back permissions and remove created object from DB:
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'admin', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

        Given path ishPath + '/' + id
        When method DELETE
        Then status 204
#       <--->



    Scenario: (-) Delete not system tag group by notadmin with permissions: Hide-View-Print

#       <--->  Create new tag group for deleting:
        * def newTagGroup = {"name":"tagName603","status":"Private","system":false,"urlPath":null,"content":null,"weight":1,"requirements":[{"type":"Assessment","mandatory":false,"limitToOneTag":true,"system":false}],"childTags":[]}

        Given path ishPath
        And request newTagGroup
        When method POST
        Then status 204

#       <--->  Login as notadmin:
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsPrint', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

#       >>> Delete created tag group by notadmin with permissions Hide-View-Print:
        Given path ishPath
        When method GET
        Then status 200
        * def id = get[0] response[?(@.name == 'tagName603')].id

        Given path ishPath + '/' + id
        When method DELETE
        Then status 403

#       <---->  Scenario have been finished. Now change back permissions and remove created object from DB:
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'admin', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

        Given path ishPath + '/' + id
        When method DELETE
        Then status 204
#       <--->



    Scenario: (-) Delete not system tag group by notadmin with permissions: Hide-View

#       <--->  Create new tag group for deleting:
        * def newTagGroup = {"name":"tagName604","status":"Private","system":false,"urlPath":null,"content":null,"weight":1,"requirements":[{"type":"Assessment","mandatory":false,"limitToOneTag":true,"system":false}],"childTags":[]}

        Given path ishPath
        And request newTagGroup
        When method POST
        Then status 204

#       <--->  Login as notadmin:
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsView', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

#       >>> Delete created tag group by notadmin with permissions Hide-View:
        Given path ishPath
        When method GET
        Then status 200
        * def id = get[0] response[?(@.name == 'tagName604')].id

        Given path ishPath + '/' + id
        When method DELETE
        Then status 403

#       <---->  Scenario have been finished. Now change back permissions and remove created object from DB:
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'admin', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

        Given path ishPath + '/' + id
        When method DELETE
        Then status 204
#       <--->



    Scenario: (-) Delete not system tag group by notadmin with permissions: Hide

#       <--->  Create new tag group for deleting:
        * def newTagGroup = {"name":"tagName605","status":"Private","system":false,"urlPath":null,"content":null,"weight":1,"requirements":[{"type":"Assessment","mandatory":false,"limitToOneTag":true,"system":false}],"childTags":[]}

        Given path ishPath
        And request newTagGroup
        When method POST
        Then status 204

#       <--->  Login as notadmin:
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsHide', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

#       >>> Delete created tag group by notadmin with permission Hide:
        Given path ishPath
        When method GET
        Then status 200
        * def id = get[0] response[?(@.name == 'tagName605')].id

        Given path ishPath + '/' + id
        When method DELETE
        Then status 403

#       <---->  Scenario have been finished. Now change back permissions and remove created object from DB:
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'admin', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

        Given path ishPath + '/' + id
        When method DELETE
        Then status 204
#       <--->



    Scenario: (-) Delete system tag group by admin

        Given path ishPath
        When method GET
        Then status 200
        * def id = get[0] response[?(@.name == 'Assessment method')].id

        Given path ishPath + '/' + id
        When method DELETE
        Then status 400
        And match response.errorMessage == "Tag group can not be deleted This tag group is required for the assessments."



    Scenario: (-) Delete NOT existing tag group

        Given path ishPath + '/99999'
        When method DELETE
        Then status 400
        And match response.errorMessage == "Tag with id:99999 doesn't exist"



    Scenario: (-) Delete tag group without any ID

            Given path ishPath + '/'
            When method DELETE
            Then status 405



    Scenario: (-) Delete tag group with NULL as ID

            Given path ishPath + '/null'
            When method DELETE
            Then status 404



    Scenario: (-) Delete tag group without path

            Given path ishPath
            When method DELETE
            Then status 405

