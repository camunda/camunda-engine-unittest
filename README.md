# camunda engine unit test template

This git repository contains a simple example of how to write a unit test for Camunda Platform. You can use it for reporting bugs or asking questions in the forums.

The project contains the following files:

```
src/
├── main
│   ├── java
│   └── resources
└── test
    ├── java
    │   └── org
    │       └── camunda
    │           └── bpm
    │               └── unittest
    │                   └── SimpleTestCase.java   (1)
    └── resources
        ├── camunda.cfg.xml                       (2)
        └── testProcess.bpmn                      (3)
```
Explanation:

* (1) A java class containing a JUnit Test. It uses the `ProcessEngineRule` for bootstrapping the process engine, as well as [camunda-bpm-assert][assert] to make your test life easier.
* (2) Configuration file for the process engine.
* (3) An example BPMN process.

## Running the test with maven

In order to run the testsuite with maven you can say:

```
mvn clean test
```

## Importing the project into eclipse.

If you use eclipse you can simply import the project by selecting `File / Import |-> Existing Maven Projects.

## Contributing

Have a look at our [contribution guide](https://github.com/camunda/camunda-bpm-platform/blob/master/CONTRIBUTING.md) for how to contribute to this repository.

## License
The source files in this repository are made available under the [Apache License Version 2.0](./LICENSE).

[assert]: https://github.com/camunda/camunda-bpm-assert