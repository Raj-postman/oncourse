@parallel=false
Feature: Main feature for all POST requests with path 'list/entity/accountTransaction'

    Background: Authorize first
        * callonce read('../../../signIn.feature')
        * url 'https://127.0.0.1:8182/a/v1'
        * def ishPath = 'list/entity/accountTransaction'
        * def ishPathLogin = 'login'
        * def ishPathPlain = 'list/plain'
        * configure httpClientClass = 'ish.oncourse.api.test.client.KarateClient'



    Scenario: (+) Create account transaction (Journal) by admin

        * def newTransaction =
        """
        {
        "fromAccount":1,
        "toAccount":2,
        "amount":111.00,
        "transactionDate":"2019-03-01"
        }
        """

        Given path ishPath
        And request newTransaction
        When method POST
        Then status 204

        Given path ishPathPlain
        And param entity = 'AccountTransaction'
        And param search = "tableName != 'O' and tableName != 'P' and tableName != 'I'"
        And param columns = "transactionDate,account.accountCode,account.description,account.type,amount,tableName"
        When method GET
        Then status 200

        * def id1 = get[0] response.rows[?(@.values == ["2019-03-01","11500","Trade debtors","asset","111.00","Journal"])].id
        * print "id1 = " + id1
        * def id2 = get[0] response.rows[?(@.values == ["2019-03-01","11100","Deposited funds","asset","-111.00","Journal"])].id
        * print "id2 = " + id2

        Given path ishPath + '/' + id1
        When method GET
        Then status 200
        And match $ == {"id":"#number","fromAccount":2,"toAccount":null,"amount":111.00,"transactionDate":"2019-03-01"}

        Given path ishPath + '/' + id2
        When method GET
        Then status 200
        And match $ == {"id":"#number","fromAccount":1,"toAccount":null,"amount":-111.00,"transactionDate":"2019-03-01"}


    Scenario: (-) Create new account transaction when Amount is null

        * def newTransaction =
        """
        {
        "fromAccount":1,
        "toAccount":2,
        "amount":null,
        "transactionDate":"2019-03-04"
        }
        """

        Given path ishPath
        And request newTransaction
        When method POST
        Then status 400
        And match $.errorMessage == "Amount is required."



    Scenario: (-) Create new account transaction when Amount is 0

        * def newTransaction =
        """
        {
        "fromAccount":1,
        "toAccount":2,
        "amount":0.00,
        "transactionDate":"2019-03-04"
        }
        """

        Given path ishPath
        And request newTransaction
        When method POST
        Then status 400
        And match $.errorMessage == "Amount is required."


    Scenario: (+) Create account transaction (Journal) by notadmin

        Given path '/logout'
        And request {}
        When method PUT
        
#       Note: Permission 'Create' is always enabled for notadmin.
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

        * def newTransaction =
        """
        {
        "fromAccount":1,
        "toAccount":2,
        "amount":222.00,
        "transactionDate":"2019-03-02"
        }
        """

        Given path ishPath
        And request newTransaction
        When method POST
        Then status 204

        Given path '/logout'
        And request {}
        When method PUT
        
#       <--->  Login as admin to view created entities:
        * def loginBody = {login: 'admin', password: 'password', kickOut: 'true', skipTfa: 'true'}

        Given path ishPathLogin
        And request loginBody
        When method PUT
        Then status 200
        And match response.loginStatus == "Login successful"
#       <--->

        Given path ishPathPlain
        And param entity = 'AccountTransaction'
        And param search = "tableName != 'O' and tableName != 'P' and tableName != 'I'"
        And param columns = "transactionDate,account.accountCode,account.description,account.type,amount,tableName"
        When method GET
        Then status 200

        * def id1 = get[0] response.rows[?(@.values == ["2019-03-02","11500","Trade debtors","asset","222.00","Journal"])].id
        * print "id1 = " + id1
        * def id2 = get[0] response.rows[?(@.values == ["2019-03-02","11100","Deposited funds","asset","-222.00","Journal"])].id
        * print "id2 = " + id2

        Given path ishPath + "/" + id1
        When method GET
        Then status 200
        And match $ contains {"id":"#number","fromAccount":2,"toAccount":null,"amount":222.00,"transactionDate":"2019-03-02"}

        Given path ishPath + "/" + id2
        When method GET
        Then status 200
        And match $ contains {"id":"#number","fromAccount":1,"toAccount":null,"amount":-222.00,"transactionDate":"2019-03-02"}


