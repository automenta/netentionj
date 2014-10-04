Web Server Features
------------------
 - Multi-client HTTP / Websocket server (via Java Netty 5.0)
     - Websockets for bidirectional asynchronous interaction
	 - HTTP streaming for high-speed unidirectional transfers
	 - REST API
 - Each client connection exclusively manages a set of NAR reasoners which provide high-level logic processing and goal-driven procedure activation.
 - High-efficiency, multi-user, multi-channel distributed event-bus for real-time pub/sub & end-to-end communication and collaboration
 - Semantic event-bus adaptively forms and suggests virtual channels for broadcasting messages to partially-relevant channels which are adjacent in semantic space
 - Data self enhances through aggregation and agent processing, allowing mere discussion to automatically evolve into actions and results.
 - Graph database (via TinkerPop Blueprints) for metrics and storage
 - Ontology that encompasses all aspects of experience by adaptively integrating DBpedia resources
 - RDF/OWL input/output in multiple formats (via Apache Jena), including N3 and JSON-LD (LinkedData) backed by rule-based reasoners which can integrate bidirectionally with NARS
 

Web Client Features
-------------------
 - High-performance HTML5/Javascript realtime websocket user-interfaces using jQuery, Bootstrap, FontAwesome, and related libraries
 - Social UI - Multi-view layout facilitating social features.  Mobile compatible
 - Desktop UI - Through an enhanced fork of DockSpawn, emulates a full-featured IDE (development environment) consisting of an arbitrary number of different panels arranged in any configuration.  
	 - Panels can displaying realtime Tag stream, contain an application widget, or embed any other type of HTML content.
	 - Multiple input, editing, and output modes for each of the various data types.
 - Mobile UI - limited functionality
 - Dashboard UI - suitable for read-only applications
 - REST API Forms - automatically generated forms for accessing each of a server's REST API endpoints


