# AEM - Automated Screenshot Generation project

This is an AEM project which has a workflow(name: Page Publish with Screenshot) that can generate Page screenshot leveraging Puppeteer Open-Source library.

## Pre-Requisite

* Setup Puppeteer based on the documentation provided in puppeteer-setup.md
* Set up a Preview Publisher running on port 4505 with agent name preview.
* Configure your local AEM with your org email service using Day CQ Mail Service.
* When you initiate a workflow ensure the user you are using is configured with an email id.

## How to use

The main parts of the Project are:

* core: Contains the code for workflow process step in com.vanguard.screenshot.gen.core.workflow package.
* ui.content: contains workflow model in conf/.../workflow directory and var/.../workflow directory.

## What does the workflow do

Workflow has the following steps:

Step 1 : Initiate workflow(name: Page Publish with Screenshot) ->

Step 2 : Publishes the page to preview AEM(Running on post 4505) ->

Step 3 : This would generate screenshot and send it to the initiator ->

Step 5 : Initiator can either approve or deny this workflow ->

* Option 1: Approve : Page gets Published
* Option 2: Reject  : Page doesnt get Published

## Build and Deploy

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

To build all the modules and deploy the `all` package to a local instance of AEM, run in the project root directory the following command:

    mvn clean install -PautoInstallSinglePackage

Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallSinglePackagePublish

Or alternatively

    mvn clean install -PautoInstallSinglePackage -Daem.port=4503

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

Or to deploy only a single content package, run in the sub-module directory (i.e `ui.apps`)

    mvn clean install -PautoInstallPackage


## LICENSE

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)