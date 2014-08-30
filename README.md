Web Server Features
------------------
 - Multi-client HTTP / Websocket server (via Java Netty 5.0)
 - Each client connection exclusively manages a set of NAR reasoners which provide high-level logic processing and goal-driven procedure activation.
 - High-efficiency message routing utilizing Tag streams that deliver messages to subscribers (ex: WebSocket clients) according to measured relevancy.
 - RDF/OWL input/output in multiple formats (via Apache Jena), including N3 and JSON-LD (LinkedData) backed by rule-based reasoners which can integrate bidirectionally with NARS
 - REST API URLs
 - Multi-user communication and collaboration, facilitated by seemingly-intelligent data processing that results from network agent activity.  This allows mere discussion to automatically evolve into actions and results.
 - Database persistence and network memory sharing


Web Client Features
-------------------
 - High-performance HTML5/Javascript realtime websocket user-interfaces using jQuery, Bootstrap, FontAwesome, and related libraries
 - Desktop UI - Through an enhanced fork of DockSpawn, emulates a full-featured IDE (development environment) consisting of an arbitrary number of different panels arranged in any configuration.  
	 - Panels can displaying realtime Tag stream, contain an application widget, or embed any other type of HTML content.
	 - Multiple input, editing, and output modes for each of the various data types.
 - Mobile UI - limited functionality
 - Dashboard UI - suitable for read-only applications
 - REST API Forms - automatically generated forms for accessing each of a server's REST API endpoints


----------


Powered by [OpenNARS](https://github.com/opennars/opennars)

![enter image description here](https://raw.githubusercontent.com/opennars/opennars/master/OpenNARS.png)

