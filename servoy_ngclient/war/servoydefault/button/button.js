angular.module('svyButton',['servoy']).directive('svyButton', function(formatFilterFilter) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
       $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
     
       $scope.$watch('model.imageMediaID', function() {
			 if ($scope.model.imageMediaID) {
       			$scope.style['background-image'] = "url('" + $scope.model.imageMediaID + "')"; 
				$scope.style['background-repeat'] = "no-repeat";
				$scope.style['background-size'] = "contain";
       		 }
       		 else {
				delete $scope.style['background-image']; 
				delete $scope.style['background-repeat'];
				delete $scope.style['background-size'];
       		 }
       })
      },
      templateUrl: 'servoydefault/button/button.html',
      replace: true
    };
  })
  
  
  
