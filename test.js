var page = require('webpage').create();
var url;

if (phantom.args) {
  url = phantom.args[0];
} else {
  url = require('system').args[1];
}

page.onConsoleMessage = function (message) {
  console.log(message);
};

function exit(code) {
  setTimeout(function(){ phantom.exit(code); }, 0);
  phantom.onError = function(){};
}

page.onAlert = function(message) {
  exit(message);
};

console.log("Loading URL: " + url);

page.open(url, function (status) {
  if (status != "success") {
    console.log('Failed to open ' + url);
    phantom.exit(1);
  }

  console.log("Running tests.");

  page.evaluate(function() {
    test.test_huh.run_tests.run(function(result ) {
      window.alert(result);
    });
  });
});
