/**
 * @module DeCentipede
 * @mail DeCentipede
 * 
 * The main entry point for the DeCentipede module
 * 
 */
var DeCentipede = (function(DeCentipede) {
	
	var jmxDomain = "no.kantega.debug";

	/**
	 * @property pluginName
	 * @type {string}
	 * 
	 * The name of this plugin
	 */
	DeCentipede.pluginName = 'decentipede';

	/**
	 * @property log
	 * @type {Logging.Logger}
	 * 
	 * This plugin's logger instance
	 */
	DeCentipede.log = Logger.get('decentipede');

	/**
	 * @property contextPath
	 * @type {string}
	 * 
	 * The top level path of this plugin on the server
	 * 
	 */
	DeCentipede.contextPath = "/decentipede-web/";

	/**
	 * @property templatePath
	 * @type {string}
	 * 
	 * The path to this plugin's partials
	 */
	DeCentipede.templatePath = DeCentipede.contextPath + "plugin/html/";

	/**
	 * @property module
	 * @type {object}
	 * 
	 * This plugin's angularjs module instance. This plugin only needs
	 * hawtioCore to run, which provides services like workspace, viewRegistry
	 * and layoutFull used by the run function
	 */
	DeCentipede.module = angular.module('decentipede',
			[ 'hawtioCore', 'ui.bootstrap.typeahead' ]).config(
			function($routeProvider) {

				/**
				 * Here we define the route for our plugin. One note is to avoid
				 * using 'otherwise', as hawtio has a handler in place when a
				 * route doesn't match any routes that routeProvider has been
				 * configured with.
				 */
				$routeProvider.when('/decentipede', {
					templateUrl : DeCentipede.templatePath + 'decentipede.html'
				});
			});

	/**
	 * Here we define any initialization to be done when this angular module is
	 * bootstrapped. In here we do a number of things:
	 * 
	 * 1. We log that we've been loaded (kinda optional) 2. We load our .css
	 * file for our views 3. We configure the viewRegistry service from hawtio
	 * for our route; in this case we use a pre-defined layout that uses the
	 * full viewing area 4. We configure our top-level tab and provide a link to
	 * our plugin. This is just a matter of adding to the workspace's
	 * topLevelTabs array.
	 */
	DeCentipede.module.run(function(workspace, viewRegistry, layoutFull) {

		DeCentipede.log.info(DeCentipede.pluginName, " loaded");

		Core.addCSS(DeCentipede.contextPath + "plugin/css/decentipede.css");

		// tell the app to use the full layout, also could use layoutTree
		// to get the JMX tree or provide a URL to a custom layout
		viewRegistry["decentipede"] = layoutFull;

		/*
		 * Set up top-level link to our plugin. Requires an object with the
		 * following attributes:
		 * 
		 * id - the ID of this plugin, used by the perspective plugin and by the
		 * preferences page content - The text or HTML that should be shown in
		 * the tab title - This will be the tab's tooltip isValid - A function
		 * that returns whether or not this plugin has functionality that can be
		 * used for the current JVM. The workspace object is passed in by
		 * hawtio's navbar controller which lets you inspect the JMX tree,
		 * however you can do any checking necessary and return a boolean href -
		 * a function that returns a link, normally you'd return a hash link
		 * like #/foo/bar but you can also return a full URL to some other site
		 * isActive - Called by hawtio's navbar to see if the current
		 * $location.url() matches up with this plugin. Here we use a helper
		 * from workspace that checks if $location.url() starts with our route.
		 */
		workspace.topLevelTabs.push({
			id : "decentipede",
			content : "DeCentipede",
			title : "Java debug agent",
			isValid : function(workspace) {
				return true; //workspace.treeContainsDomainAndProperties(jmxDomain);
			},
			href : function() {
				return "#/decentipede";
			},
			isActive : function(workspace) {
				return workspace.isLinkActive("decentipede");
			}

		});

	});

	/**
	 * @function DeCentipedeController
	 * @param $scope
	 * @param jolokia
	 * 
	 * The controller for simple.html, only requires the jolokia service from
	 * hawtioCore
	 * 
	 */
	DeCentipede.DeCentipedeController = function($scope, jolokia, $http) {

		var loadedClasses = [];
		$scope.showAddClassControl = false;
		$scope.agentAvailable = false;
		$scope.showSelectResourceRootsControl = false;
		$scope.showAddImplementorsControl = false;
		$scope.implementors = {};
		
		$scope.table = {
			properties : {
				EmitWalkbacks : {
					label : "Emit walkbacks",
					tooltip : 'The agent should produce walkbacks on exceptions and breakpoints',
					type : 'boolean'
				},
				NullPointerDiagnosed : {
					label : "Diagnose NullPointerExceptions",
					tooltip : 'The agent will attempt to enrich NullPointerExceptions with details regarding the cause',
					type : 'boolean'
				}
			}
		};
		var lastResult=[];
		$scope.loadedClasses = function(viewValue) {
			if(viewValue === undefined) {
				return [];
			}
			if(!$scope.running || viewValue.length < 3) {
				return [viewValue];
			}
			var fromJolokia= jolokia.request( {
				type : "exec",
				operation : "candidateClassesForFilter(java.lang.String)",
				arguments: [viewValue],
				mbean : mbean
			} );

			return fromJolokia.value;
		};
		
		function monitorClass(className) {
			jolokia.request( {
				type : "exec",
				operation : "monitorClass(java.lang.String)",
				arguments: [className],
				mbean : mbean
			} );
			$scope.classToAdd='';
			$scope.agent.MonitoredClasses.push(className);
			$scope.agent.MonitoredClasses.sort();
			$scope.showAddClassControl=false;
			Core.$apply($scope);
		}
		
		$scope.showAddClass = function() {
			$scope.showAddClassControl=true;
			Core.$apply($scope);
		};
		
		$scope.showAddResourceRoots = function() {
			$scope.showAddResourceRootsControl=true;
			Core.$apply($scope);
		};
		
		$scope.stopMonitoringClass = function(className) {
			jolokia.request( {
				type : "exec",
				operation : "stopMonitoringClass(java.lang.String)",
				arguments: [className],
				mbean : mbean
			} );

		};
		
		$scope.showWalkback = function(walkback) {
			alert(walkback);
		};
		
		$scope.onSelect = function($item, $model, $label) {
			monitorClass($item);
		};
		
		$scope.onSelectResource = function($item, $model, $label) {
			$scope.showImplementors($item);
		};
		
		$scope.showResourceRoots = function($item, $model, $label) {
			$scope.showSelectResourceRootsControl = true;
			$scope.showAddImplementorsControl = false;
			Core.$apply($scope);
		};
		
		$scope.showImplementors = function (resourceRoot) {
			var implementorMap = jolokia.request( {
				type : "exec",
				operation : "getImplementorsAndCounts(java.lang.String)",
				arguments: [resourceRoot],
				mbean : mbean
			}).value;
			
			DeCentipede.log.info("Retrieved implementors for " + resourceRoot + " : " + Object.keys(implementorMap));
			$scope.implementors = implementorMap; //[];
//			for(var prop in Object.keys(implementorMap)) {
//				if(implementorMap.hasOwnProperty(prop)) {
//					var implementor = {"class" : prop , "count" : implementorMap[prop]};
//					DeCentipede.log.debug("Pushing " + implementor)
//					$scope.implementors.push(implementor);
//				}
//			}
			$scope.showSelectResourceRootsControl = false;
			$scope.showAddImplementorsControl = true;
			Core.$apply($scope);
		};
		
		$scope.monitorClass = function() {
			monitorClass($scope.classToAdd);
		};
		
		$scope.monitorImplementor = function(implementor) {
			monitorClass(implementor);
			$scope.showAddImplementorsControl = false;
			Core.$apply($scope);
		};


		$scope.agent = {};
		var mirroredAttributes = [ 'NullPointerDiagnosed', 'EmitWalkbacks' , 'ResourceRootCandidates'];
		var isSettingUi = true;

		var mbean = 'no.kantega.debug:type=DebugAgent';

		// set up watch to reflect changes back to jolokia immediately
		for (index = 0; index < mirroredAttributes.length; index++) {
			var attribute = mirroredAttributes[index];
			$scope.$watch('agent.' + attribute, function(newValue, oldValue,
					scope) {
				// ensure that we do not update based on updates from jolokia
				if (!isSettingUi && jolokia) {
					DeCentipede.log.info("Sending updates to jolokia")
					var requests = [];
					for (i = 0; i < mirroredAttributes.length; i++) {
						requests.push({
							type : 'write',
							mbean : mbean,
							attribute : mirroredAttributes[i],
							value : $scope.agent[mirroredAttributes[i]]
						})
					}
					//
					jolokia.request(requests, onSuccess(function() {
						DeCentipede.log.info("Updated attributes in jolokia");
					}))
				}
			});
		}

		var callOperation = function(opName) {
			DeCentipede.log.info(Date.now() + " invoking operation " + opName
					+ " on decentipede");
			jolokia.request([ {
				type : "exec",
				operation : opName,
				mbean : mbean
			} ], onSuccess(function() {
				// on success we may assume that running state is changed
				$scope.running = !$scope.running;
				Core.$apply($scope);
				DeCentipede.log.info(Date.now() + " Operation " + opName
						+ " was successful");
			}));
		};

		$scope.pause = function() {
			callOperation("stop()");
		};

		$scope.start = function() {
			callOperation("start()");
		};
		
		$scope.install = function() {
			var url='../decentipede-web/install?pid=' + findMyPid();
			$http({
				  method: 'POST',
				  url: url
				}).then(function successCallback(response) {
				    DeCentipede.log.info('Installed agent');
				  }, function errorCallback(response) {
				    DeCentipede.log.info('Failure installing agent: ' + response);
				  });

		};

		function findMyPid() {
			//TODO: Semi dirty to snatch this from the title, should ideally have a backup plan
			var regex=/pid:(\d+)/g;
			var pid = regex.exec(window.document.title);
			return pid[1];
		}

		// register a watch with jolokia on this mbean to
		// get updated metrics
		Core.register(jolokia, $scope, {
			type : 'read',
			mbean : mbean,
			arguments : []
		}, onSuccess(renderCentipede), agentMissing);

		function agentMissing(response) {
			DeCentipede.log.error("agent not available " + response);
		}

		function renderCentipede(response) {
			isSettingUi = true;
			$scope.agentAvailable = true; //workspace.treeContainsDomainAndProperties(jmxDomain);
			DeCentipede.log.debug(Date.now() + " Updating values from JMX");
			$scope.agent = response.value;
			$scope.running = response.value['Running'];
			Core.$apply($scope);
			isSettingUi = false;
		}

	};

	return DeCentipede;

})(DeCentipede || {});

// tell the hawtio plugin loader about our plugin so it can be
// bootstrapped with the rest of angular
hawtioPluginLoader.addModule(DeCentipede.pluginName);
