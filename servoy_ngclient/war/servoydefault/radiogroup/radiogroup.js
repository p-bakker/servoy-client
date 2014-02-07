servoyModule.directive('svyRadiogroup', function($utils) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
          $scope.notNull = $utils.notNull // TODO remove the need for this
      },
      templateUrl: 'servoydefault/radiogroup/radiogroup.html',
      replace: true
    };
  })

  
  
  
  
