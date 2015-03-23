angular.module('testservice',['servoy'])
.factory("testservice",function($window,$services) {
	var scope = $services.getServiceScope('testservice');
	return {
		/**
		 * Display an alert box.
		 * @example 
		 * plugins.testservice.text = "something"
		 * plugins.testservice.talk()
		 */
		talk: function() {
			alert("talk: " + scope.model.text);
			scope.model.text = "something else"
		}
	}
})
.run(function($rootScope,$services)
{
	var scope = $services.getServiceScope('testservice')
	scope.$watch('model', function(newvalue,oldvalue) {
		// handle state changes
		console.log(newvalue)
	}, true);
})