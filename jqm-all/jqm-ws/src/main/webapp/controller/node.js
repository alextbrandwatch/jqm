'use strict';

var jqmControllers = angular.module('jqmControllers', [ 'jqmConstants', 'jqmServices', 'ui.bootstrap', 'jqm.editors' ]);

jqmControllers
		.controller(
				'µNodeListCtrl',
				function($scope, $http, $uibModal, µNodeDto, jqmCellTemplateBoolean, jqmCellEditorTemplateBoolean, uiGridConstants) {
					$scope.items = null;
					$scope.selected = [];

					$scope.save = function() {
						// Save and refresh the table - ID may have been
						// generated by the server.
						µNodeDto.saveAll({}, $scope.items, $scope.refresh);
					};

					$scope.refresh = function() {
						$scope.selected.length = 0;
						$scope.items = µNodeDto.query();
					};

					// Only remove from list - save() will sync the list with
					// the server so no need to delete it from server now
					$scope.remove = function() {
						var q = null;
						for (var i = 0; i < $scope.selected.length; i++) {
							q = $scope.selected[i];
							$scope.items.splice($scope.items.indexOf(q), 1);
						}
						$scope.selected.length = 0;
					};

					$scope.gridOptions = {
						data : 'items',

						enableSelectAll : false,
						enableRowSelection : true,
						enableRowHeaderSelection : false,
						enableFullRowSelection : true,
						enableFooterTotalSelected : false,
						multiSelect : false,
						enableSelectionBatchEvent: false,
						noUnselect: true,
						
						enableColumnMenus : false,
						enableCellEditOnFocus : true,
						virtualizationThreshold : 20,
						enableHorizontalScrollbar : 0,

						onRegisterApi : function(gridApi) {
							$scope.gridApi = gridApi;
							gridApi.selection.on.rowSelectionChanged($scope, function(rows) {
								$scope.selected = gridApi.selection.getSelectedRows();
							});
							
							gridApi.cellNav.on.navigate($scope,function(newRowCol, oldRowCol){
								if (newRowCol !== oldRowCol)
								{
									gridApi.selection.selectRow(newRowCol.row.entity);
								}
				            });
							
							$scope.gridApi.grid.registerRowsProcessor(createGlobalFilter($scope, [ 'name', 'dns' ]), 200);
						},

						columnDefs : [
								{
									field : 'name',
									displayName : 'Name',
									width : '**',
									cellTemplate : '<div ng-class="{\'bg-success\': row.entity[\'reportsRunning\'] === true, \'bg-danger\': row.entity[\'reportsRunning\'] === false}"> \
													<div class="ui-grid-cell-contents">{{row.entity[col.field]}}</div></div>',
									sort : {
										direction : uiGridConstants.DESC,
										priority : 0
									},
								}, {
									field : 'dns',
									displayName : 'DNS to bind to',
									width : '**',
								}, {
									field : 'port',
									displayName : 'HTTP port',
									width : '*',
								}, {
									field : 'outputDirectory',
									displayName : 'File produced storage',
									width : '***',
								}, {
									field : 'jobRepoDirectory',
									displayName : 'Directory containing jars',
									width : '***',
								}, {
									field : 'tmpDirectory',
									displayName : 'Temporary directory',
									width : '**',
								}, {
									field : 'rootLogLevel',
									displayName : 'Log level',
								}, {
									field : 'jmxRegistryPort',
									displayName : 'jmxRegistryPort',
								}, {
									field : 'jmxServerPort',
									displayName : 'jmxServerPort',
								}, {
									field : 'enabled',
									displayName : 'Enabled',
									cellTemplate : jqmCellTemplateBoolean,
									editableCellTemplate : jqmCellEditorTemplateBoolean,
									width : '*',
								}, {
									field : 'loapApiSimple',
									displayName : 'Simple API',
									cellTemplate : jqmCellTemplateBoolean,
									editableCellTemplate : jqmCellEditorTemplateBoolean,
									width : '*',
								}, {
									field : 'loadApiClient',
									displayName : 'Client API',
									cellTemplate : jqmCellTemplateBoolean,
									editableCellTemplate : jqmCellEditorTemplateBoolean,
									width : '*',
								}, {
									field : 'loadApiAdmin',
									displayName : 'Admin API',
									cellTemplate : jqmCellTemplateBoolean,
									editableCellTemplate : jqmCellEditorTemplateBoolean,
									width : '*',
								}, ]
					};

					$scope.stop = function() {
						var q = null;
						for (var i = 0; i < $scope.selected.length; i++) {
							q = $scope.selected[i];
							q.stop = true;
							q.$save();
						}
					};

					$scope.showlog = function(nodeName) {
						$uibModal.open({
							templateUrl : './template/file_reader.html',
							controller : 'fileReader',
							size : 'lg',

							resolve : {
								url : function() {
									return "ws/admin/node/" + nodeName + "/log?latest=" + 200;
								}
							},
						});
					};

					$scope.refresh();
				});

jqmControllers.controller('µNodeDetailCtrl', [ '$scope', '$routeParams', 'µNodeDto', function($scope, $routeParams, µNodeDto) {
	$scope.nodeId = $routeParams.nodeId;
	$scope.error = null;

	$scope.onError = function(errorResult) {
		console.debug(errorResult);
		$scope.error = errorResult.data;
	};

	$scope.node = µNodeDto.get({
		id : $routeParams.nodeId
	}, function() {
	}, $scope.onError);

} ]);
