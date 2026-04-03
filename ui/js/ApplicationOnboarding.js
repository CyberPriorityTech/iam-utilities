angular.module('ApplicationOnboarding',['ui.bootstrap','sailpoint.dataview'])
.config(['$httpProvider',function($httpProvider) {
    $httpProvider.defaults.xsrfCookieName = "CSRF-TOKEN";         
}])

.controller('AppOnoardingPluginFullPageCtrl', ['$scope','$http', function($scope, $http) {

   // Variables
   $scope.applicationOptions = [];  
   $scope.dropdownOptions = [];
    // list of applications
   $scope.results = [];
   $scope.showDiv = false;
   $scope.csvFile = null;

   // Fetch application names from IIQ REST API
   $http.get(PluginHelper.getPluginRestUrl("ApplicationOnboarding/search/applications"))
       .then(function(response) {
       
       try {
  	      $scope.applicationOptions = response.data;
          response.data.forEach(function(item) {
       	     $scope.dropdownOptions.push({
           		displayName: item,
           		id: item
           	 });
          });
	   } catch(err) {
          $scope.directions= "Search error!";
          $scope.objects = ["Search error: could not parse response"];
               
       }
       }, function(error) {
           console.error("Error fetching applications", error);
           $scope.applicationOptions = [];
       });

   // Upload CSV handler
   $scope.uploadCSV = function () {
       if (!$scope.csvFile) {
           alert("Please select a CSV file");
           return;
       }
       if (!$scope.object) {   // ng-model bound to dropdown
        alert("Please select an Application Name");
        return;
    }
       var reader = new FileReader();
       reader.onload = function(e) {
           var fileContent = e.target.result;
           $http.post(PluginHelper.getPluginRestUrl("ApplicationOnboarding/importCSV"), {
               csvData: fileContent,
               applicationName: $scope.object
           }).then(function(response){
               $scope.results = response.data;
               $scope.showDiv = true;
           }, function(error){
               console.error(error);
               alert("CSV Upload Failed");
           });
       };
       reader.readAsText($scope.csvFile);
   };

}]);

// Directive for file input binding
angular.module('ApplicationOnboarding').directive('fileModel', ['$parse', function ($parse) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var model = $parse(attrs.fileModel);
            element.bind('change', function () {
                scope.$apply(function () {
                    model.assign(scope, element[0].files[0]);
                });
            });
        }
    };
}]);
