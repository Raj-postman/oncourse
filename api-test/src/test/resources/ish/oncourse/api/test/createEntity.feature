@ignore
@parallel=false
Feature: re-usable feature to create new entity
    
    Scenario:
        * url 'https://127.0.0.1:8182/a/v1'
        * def data = {path: '#(path)', entity: '#(entity)'}
        Given path data.path
        And request data.entity
        When method POST