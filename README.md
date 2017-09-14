# Browser2Rest Tests #

This is a test project that uses Protractor and ExpressJS to run integration tests on ProductPlan's frontend.

## Setup ##

Note: The following has been added to the general Product Plan setup script. If you have run that, you shouldn't have to do these steps.

Install Java SDK:

    choco install jdk8

Add the following to your path:

    C:\Program Files\Java\jdk1.8.0_25\bin

Install gulp globally:

    npm install -g gulp

Install Google Chrome Portable:

    .\tools\Initialize-GoogleChromePortable.ps1

## Updating Protractor via npm ##

Sometimes you want to update protractor, to use new features and such, feel free, here's how: 

* Run npm update protractor@x.x.x --save-dev.
* Run webdriver-manager update
* Verify that protractor-config.ts property seleniumServerJar points to correct jar file (maybe version got updated). 
* Run suite locally to verify.
* Push node_modules and perhaps PP if need be.

## Writing ##

The best way to get an idea of how to write the tests is to look at some existing tests as examples. `viewHeaderTitleSpec.ts` is a very basic example that loads the department view and verifies that the text "Product Plan" exists in the header.

#### RestApiFake ####

A RestApiFake instance must be created before each test, and disposed after each test. The RestApiFake starts a web server that acts as a mock of ProductPlan's backend. It also exposes a couple of methods that lets us control what URLs it should listen on and what data it should send to the browser.

The following is a minimal setup for a test. The `setupBaseline()` call ensures that all necessary calls to render the department view are responded to with a minimum amount of data.

    var apiFake: testutils.RestApiFake;

    beforeEach(() => {
        apiFake = new testutils.RestApiFake(browser).setupBaseline();
    });

    afterEach(() => {
        apiFake.dispose();
    });

Depending on the test we can then setup more responses or override the baseline:

    apiFake.setupApiGet(/getproducts/, { name: "foobar", id: 99 });

    apiFake.setupApiGet(/getproducts/, (req: express.Request) => {
        var name = req.query.name;
        return { name: name, id: 99 };
    });

#### performance testing ####

There is a small library for testing javascript performance.

It is used like this:
(lines taken from expandCollapseProductsSpec.ts)

    var perfContext: performanceContext.Context;
    var navigationPromise: webdriver.promise.Promise<void>;
    …
    beforeEach(() => {
    …
        perfContext.start();
        navigationPromise = testutils.browseToProductPlan();
    });

    it("should finish navigation in a reasonable time", () => {
        var elapsed = perfContext.getSecondsElapsedSinceStart(navigationPromise);
        expect(elapsed).toBeLessThan(7);
    });

    …

    describe("When expanding all products with expand/collapse all button:",() => {
        var collapseFinishedPromise: webdriver.promise.Promise<void>;

        beforeEach(() => {
            expandCollapseAllProductsButton = timeline.getExpandCollapseAllProductsButton();

            perfContext.start();
            collapseFinishedPromise = expandCollapseAllProductsButton.click();
        });

        it("should collapse within a reasonable time", () => {
            var collapseTime = perfContext.getSecondsElapsedSinceStart(collapseFinishedPromise);
            expect(collapseTime).toBeLessThan(4);
        }); 

`start()` starts the timer
`getSecondsElapsedSinceStart(promise)` gets a promise of the number of seconds that has passed from `start()` until the promise given to the method is finished.
The output can be used in a expect statement (see above. In protractor `expect` accepts promises)
The number of seconds is a decimal number, with high precision. Times are logged to console.


#### Organization of tests ####

The tests should be put in subfolders to the folder `scenario`. The folder structure should mimic the folder structure below `Presentation` in the Web project. (And thus also mimic the modules created there).

If you create a new folder under `scenarios` you should also create a corresponding new suite in `protractor-config.ts`, by adding to this option:

    suites: {
        productView: ["scenarios/ProductView/**/*Spec.js"],
        buyingStatus: "scenarios/BuyingStatus/*Spec.js",
        timeline: "scenarios/Grid/Timeline/**/*Spec.js",
        validPlanLevel: "scenarios/Grid/Timeline/ValidPlanLevel/*Spec.js",
        saveToPles: "scenarios/SaveToPles/*Spec.js",
        sellPriceVsInPrice: "scenarios/SellPriceVsInPrice/*Spec.js",
        setWorkingArea: "scenarios/SetWorkingArea/*Spec.js",
        windowHandling: "scenarios/WindowHandling/*Spec.js"
    },

If you need to create new mocked data, you should create factories in the `mockdata` folder.

You should try to write your tests in a fluent, natural language. Hide css-selectors, input interaction and similar technical details in abstract methods that describe what you are trying to achieve. Put these methods in the appropriate context classes in the `context` folder.

#### Protractor promises ####

    browser.get("#/HM/Store/1-2015/98/1017").then(() => {
        // place code here that should run after the browser has finished navigating
    });

    element(by.buttonText("Save")).click().then(() => {
        // place code here that should run after the browser has done the click
    });

## Running ##

The tests are run through Gulp, which compiles typescript, creates an html file and other necessary processes automatically before starting up Protractor.

#### Run jasmine specifications ####

    gulp run-unittests

or 

    gulp watch-unittests

Runs / Watches all jasmine specifications.

#### Run protractor scenarios ####

    gulp run-browser2rest

or

    gulp watch-browser2rest

Runs / Watches all tests in all files inside the folder `./scenarios`.

#### Run a single test file ####

    gulp run-browser2rest --specs folderUnderScenarios/myScenarioSpec.js

Runs only the tests in the given file, assuming it's located inside the folder `./scenarios`.

#### Run a suite of tests ####

    gulp run-browser2rest --suite timeline

Runs the tests in the given suite. The available suites are listed in `./protractor-config`.

## Debugging ##

When debugging, start the tests with the `--debug` flag:

    gulp run-browser2rest --specs SaveToPles/saveToPlesSpec.js --debug

#### Node debugger ####

In the spec file:

    browser.get("#/HM/Store/1-2015/98/1017").then(() => {
        debugger; // Break point
    });


Now protractor will run in the node debug console:
Type `c` to continue to the next break point.
Type `repl` to enter a REPL mode where you can evaluate variables in the scope of the spec file.

#### Sleeping the browser ####

In the spec file:

    browser.get("#my/url").then(() => {
        browser.sleep(10000); // 10 second delay before continuing.
    });

#### Pausing the browser ####

In the spec file:

    browser.get("#my/url").then(() => {
        browser.pause();
    });

This pauses the browser at the given point of your test. Then you can open up the developer console in your browser, for example. To continue you need to type `c` in the console. Unfortunately, it seems that Protractor and Selenium have trouble resuming the test procedure after this happens and you will likely need to restart the tests afterwards.

**Don't commit pauses.** It will be annoying for others to run the tests if the code is littered with these statements.

#### Debugging browser side with breakpoints etc ####

We have not been able to get this working :-(

## TeamCity ##

The Browser2Rest integration tests are run continuously by [a TeamCity build](http://teamcity/viewType.html?buildTypeId=ProductPlan_ProductPlanBrowser2RestIntegrationTests).

The `Initialize-GoogleChromePortable.ps1` is run by both developers and by TeamCity. The script copies Google Chrome Portable from the share `\secc108\Projects\PLES Development\ProductPlan\UsedByTeamCity\GoogleChromePortable\`, where you should always have the appropriate Google Chrome Portable version.
If you need to add [new versions they can be downloaded here](http://portableapps.com/apps/internet/google_chrome_portable). [Old versions can be downloaded here](http://mirror.ufs.ac.za/portableapps/Google%20Chrome%20Portable/). You should not lightly remove old versions, because they might be needed by old builds, e.g. Release builds.



## Links ##

- [Protractor API reference](http://angular.github.io/protractor/#/api)
