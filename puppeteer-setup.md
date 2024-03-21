# Setting up Puppeteer

Puppeteer (https://pptr.dev/) is an open-source library that leverages headless chromium to generate page screenshots.

## Pre-Requisites

Node and following modules needs to be installed to use the puppeteer:

* chromiun
* chromium-headless
* node
* npm
* express
* puppeteer

## How to build

We need to write javascript to be able to leverage these libraries and generate screenshot, sample JS:

```
const puppeteer = require('puppeteer');
//const pTimeout = require("p-timeout");


// we're using async/await - so we need an async function, that we can run
const run = async () => {
    // open the browser and prepare a page
    //const browser = await puppeteer.launch({defaultViewport: {width: 1280, height: 800}, args: ['--no-sandbox', '--ignore-certificate-errors','--ignore-ssl-errors', '--disable-setuid-sandbox','--disable-web-security','--font-render-hinting=medium'], executablePath:'/usr/lib64/chromium-browser/headless_shell'});
    const browser = await puppeteer.launch({defaultViewport: {width: 1280, height: 800}, args: ['--no-sandbox', '--ignore-certificate-errors','--ignore-ssl-errors', '--disable-setuid-sandbox','--disable-web-security','--font-render-hinting=medium'], executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'});
    const page = await browser.newPage();

    const textRegex = /(javascript|html)/; // example regex
    // it's better to emulate before going to the page, because otherwise a resize would happen
    //await page.emulate(puppeteer.devices['iPad']);

    await page.emulate({
        name: 'Desktop 1920x1080',
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.75 Safari/537.36',
        viewport: {
            width: 1920,
            height: 1080
        }
    });

    //await page.setRequestInterception(true);

    page.on('response', (response) => {
        const headers = response.headers();
        console.log("Response: " + response.url());

        // example test: check if content-type contains javascript or html
        const contentType = headers['content-type'];
        if (textRegex.test(contentType)) {
        }
    });

    await page.goto('http://localhost:4505/content/we-retail/us/en.html',{ waitUntil: "networkidle2"});
        await page.screenshot({
            path: 'test_screenshot.png',
            fullPage: true,
        });
    await page.pdf({ path: 'test_screenshot.pdf', format: 'letter', displayHeaderFooter: true, printBackground: true });

    // close the browser
    await browser.close();
};

// run the async function
run();

```

The above code generates screenshot an absolute URL defined, but we can write a method that takes url as an input URL and passes that on to generate screenshot.


