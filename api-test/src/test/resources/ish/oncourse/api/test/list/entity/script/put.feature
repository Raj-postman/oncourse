@parallel=false
Feature: Main feature for all PUT requests with path 'list/entity/script' without license

    Background: Authorize first
        * call read('../../../signIn.feature')
        * url 'https://127.0.0.1:8182/a/v1'
        * configure httpClientClass = 'ish.oncourse.api.test.client.KarateClient'
        * def ishPath = 'list/entity/script'
        * def ishPathPlain = 'list/plain'
        * def ishPathLogin = 'login'

        

    Scenario: (+) Update script (Query panel) by admin

#       <----->  Add a new entity to update and define id:
        * def scriptWithQueryPanel = {"keyCode":"test.script_query","name":"script with Query panel","enabled":true,"content":"\n// Query closure start \n  def result = query {\n    entity \"Room\"\n    query \"createdOn is last year\"\n    context args.context\n  }      \n  // Query closure end\n","trigger":{"type":"On demand"},"description":"some description"}
        Given path ishPath
        And request scriptWithQueryPanel
        When method POST
        Then status 204

        Given path ishPathPlain
        And param entity = 'Script'
        And param search = 'name == "script with Query panel"'
        And param columns = 'name'
        When method GET
        Then status 200
        * def id = response.rows[0].id
#       <--->

        * def scriptToUpdate = {"keyCode":"test.script_query","name":"script with Query panel_upd","description":"some description_upd","enabled":false,"trigger":{"type":"Class cancelled","entityName":null,"cron":null},"content":"\n// Query closure start \n  def result = query {\n    entity \"Room\"\n    query \"createdOn is last week\"\n    context args.context\n  }      \n  // Query closure end\n"}

        Given path ishPath + '/' + id
        And request scriptToUpdate
        When method PUT
        Then status 204

        Given path ishPath + '/' + id
        When method GET
        Then status 200
        And match $ contains
        """
        {
        "id":"#number",
        "name":"script with Query panel_upd",
        "description":"some description_upd",
        "enabled":false,
        "trigger":{"type":"Class cancelled","entityName":null,"cron":null},
        "content":"#string",
        "lastRun":[],
        "keyCode":"test.script_query",
        "createdOn":"#ignore",
        "modifiedOn":"#ignore",
        "variables":[],
        "options":[]
        }
        """




#    Scenario: (-) Update script (Email panel) by notadmin without access rights



    Scenario: (-) Update script (Import panel) by admin

        * def scriptToUpdate = {"keyCode":"test.script_query","name":"script with Query panel_upd23","description":"some description_upd","enabled":false,"trigger":{"type":"Class cancelled","entityName":null,"cron":null},"content":"import a.b.cdef\ndef run(args) {\n\n// Query closure start \n  def result = query {\n    entity \"Site\"\n    query \"createdOn today \"\n    context args.context\n  }      \n  // Query closure end\n}"}

        Given path ishPathPlain
        And param entity = 'Script'
        And param search = 'name == "script with Query panel_upd"'
        And param columns = 'name'
        When method GET
        Then status 200
        * def id = response.rows[0].id

        Given path ishPath + '/' + id
        And request scriptToUpdate
        When method PUT
        Then status 204



    Scenario: (-) Update script (Script panel) by admin

        * def scriptToUpdate = {"keyCode":"test.script_query","name":"script with Query panel_upd","description":"some description_upd","enabled":false,"trigger":{"type":"Class cancelled","entityName":null,"cron":null},"content":"\ndef run(args) {\n\n new Date(); \n // Query closure start \n  def result = query {\n    entity \"Site\"\n    query \"createdOn today \"\n    context args.context\n  }      \n  // Query closure end\n}"}

        Given path ishPathPlain
        And param entity = 'Script'
        And param search = 'name == "script with Query panel_upd23"'
        And param columns = 'name'
        When method GET
        Then status 200
        * def id = response.rows[0].id

        Given path ishPath + '/' + id
        And request scriptToUpdate
        When method PUT
        Then status 204



    Scenario: (-) Update script (Import panel) by notadmin with access rights
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsDelete', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

        * def scriptToUpdate = {"keyCode":"test.script_query","name":"script with Query panel_upd","description":"some description_upd","enabled":false,"trigger":{"type":"Class cancelled","entityName":null,"cron":null},"content":"import a.b.cdef\ndef run(args) {\n\n// Query closure start \n  def result = query {\n    entity \"Site\"\n    query \"createdOn today \"\n    context args.context\n  }      \n  // Query closure end\n}"}

        Given path ishPathPlain
        And param entity = 'Script'
        And param search = 'name == "script with Query panel_upd"'
        And param columns = 'name'
        When method GET
        Then status 200
        * def id = response.rows[0].id

        Given path ishPath + '/' + id
        And request scriptToUpdate
        When method PUT
        Then status 204



    Scenario: (-) Update script (Script panel) by notadmin with access rights

        * def scriptToUpdate = {"keyCode":"test.script_query","name":"script with Query panel_upd","description":"some description_upd","enabled":false,"trigger":{"type":"Class cancelled","entityName":null,"cron":null},"content":"\ndef run(args) {\n\n new Date(); \n // Query closure start \n  def result = query {\n    entity \"Site\"\n    query \"createdOn today \"\n    context args.context\n  }      \n  // Query closure end\n}"}

        Given path ishPathPlain
        And param entity = 'Script'
        And param search = 'name == "script with Query panel_upd"'
        And param columns = 'name'
        When method GET
        Then status 200
        * def id = response.rows[0].id

        Given path ishPath + '/' + id
        And request scriptToUpdate
        When method PUT
        Then status 204



    Scenario: (-) Update script (all panels) by admin

        * def scriptToUpdate = {"keyCode":"test.script_query","name":"script with Query panel_upd","description":"some description_upd","enabled":false,"trigger":{"type":"Class cancelled","entityName":null,"cron":null},"content":"import a.b.cdef\ndef run(args) {\n\n new Date(); \n // Query closure start \n  def result = query {\n    entity \"Site\"\n    query \"createdOn today \"\n    context args.context\n  }      \n  // Query closure end\n}"}

        Given path ishPathPlain
        And param entity = 'Script'
        And param search = 'name == "script with Query panel_upd"'
        And param columns = 'name'
        When method GET
        Then status 200
        * def id = response.rows[0].id

        Given path ishPath + '/' + id
        And request scriptToUpdate
        When method PUT
        Then status 204



    Scenario: (-) Update script (all panels) by notadmin with access rights
        Given path '/logout'
        And request {}
        When method PUT
        * def loginBody = {login: 'UserWithRightsDelete', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

        * def scriptToUpdate = {"keyCode":"test.script_query","name":"script with Query panel_upd","description":"some description_upd","enabled":false,"trigger":{"type":"Class cancelled","entityName":null,"cron":null},"content":"import a.b.cdef\ndef run(args) {\n\n new Date(); \n // Query closure start \n  def result = query {\n    entity \"Site\"\n    query \"createdOn today \"\n    context args.context\n  }      \n  // Query closure end\n}"}

        Given path ishPathPlain
        And param entity = 'Script'
        And param search = 'name == "script with Query panel_upd"'
        And param columns = 'name'
        When method GET
        Then status 200
        * def id = response.rows[0].id

        Given path ishPath + '/' + id
        And request scriptToUpdate
        When method PUT
        Then status 204

#       <----->  Scenario have been finished. Now find and remove created object from DB:
        * def loginBody = {login: 'admin', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"

        Given path ishPath + "/" + id
        When method DELETE
        Then status 204
