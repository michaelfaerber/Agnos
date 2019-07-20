# kg-agnostic-entity-linking
<body>
<br>
<h2> Licenses </h2>
 <table style="width:100%">
  <tr>
    <th>Sub-Project Name</th>
    <th>License Info</th>
    <th>JAR/Source/Dependency Location</th>
  </tr>
  <tr>
    <td>RBBNPE (NPComplete)</td>
    <td>Copyright GPLv3, Laurenz Vorderwoelbecke &amp; Michael Färber</td>
    <td>
    lib/npcomplete.jar;</br>
    bmw.annprocessor.executable.NPExtractionManager;<br>
    bmw.annprocessor.executable.NPExtractionManagerTest
    </td>
  </tr>
  <tr>
    <td><a href="https://github.com/WDAqua/PageRankRDF">PageRankRDF</a></td>
    <td><a href="https://github.com/WDAqua/PageRankRDF/blob/master/LICENSE">MIT License</a></td>
    <td>eu.wdaqua</td>
  </tr>
  <tr>
    <td><a href="https://github.com/tdebatty/java-LSH">java-LSH</a></td>
    <td><a href="https://github.com/tdebatty/java-LSH/blob/master/LICENSE.md">MIT License</a></td>
    <td>LSHMinhash</td>
  </tr>
  <tr>
  	<td><a href="http://data.dws.informatik.uni-mannheim.de/rdf2vec/">RDF2Vec</td>
  	<td>/</td>
  	<td>de.dwslab.petar</td>
  </tr>
</table> 


<h2>Guide(s)</h2>
<h3>Setting up for a new KG</h3>
Introduce it as an additional enumeration within EnumModelType and define a SPARQL query (done in EntityQuery.java) for it to fetch its entities.
</br>
<h3>Set up precomputation structures</h3>
<dl>

<dt>1) Importing KG, setting up mention detection & pagerank, then computing embeddings</dt>
	<dd>1. Import KG into Jena (local) or Virtuoso (possible remote connection through JDBC driver) by executing LauncherSetupTDB, specifying the wanted EnumModelType (aka. KG) and input location in order to set up the TDB for Jena usage. If the KG is accessed via the Virtuoso endpoint/JDBC driver (and is already loaded within it), this step is not necessary.</dd>
	<dd>2. Execute LauncherExecuteQueries, specifying the wanted EnumModelType (aka. KG) in order to execute all wanted surface form, helping surface form etc. queries on the KG - required for mention detection and for deciding which embeddings are required.</dd>
	<dd>3. Execute LauncherSetup, specifying the wanted EnumModelType (aka. KG) - this will execute the mention detection setup, as well as compute apriori PageRank values for the KG file located as defined in FilePaths.FILE_PAGERANK. <b>Note</b> that a local version of the KG needs to be present (even if using Virtuoso) in order to compute PageRank on it.</dd>
	<dd>4. Compute Graph Walks (Java) with wanted arguments in LauncherWalkGenerator and then executing it, outputting walks to ./[KG]/resources/data/walks.txt.</dd>
	<dd>5. Specify location of output graph walks in ./sentencesPaths.txt (can be split among multiple files)</dd>
	<dd>6. Specify hyperparameters and compute Embeddings (Python) by executing ./scripts/trainModel.py</dd>
	<dd>7. Place/Move embeddings appropriately into the KG's file tree structure as defined in FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS (or change the value to match the embeddings output location).</dd>

<dt>2) (Optional) Mention Detection Tuning (LSH)</dt> 
	<dd>0. Execute LauncherMentionDetectionTuning, defining a sample input as well as potentially different bins and bands dimensions in order to tune LSH arguments for execution times (not inclusiveness/exclusiveness/quality of results). Then adapt the arguments LSH_BANDS and LSH_BUCKETS in Numbers.java appropriately.</dd>

<dt>3) Done. Ready to apply entity linking!</dt>

</dl>

<h3>Setup of virt-jena drivers (dependency required for Virtuoso execution) on a local repository (due to unavailability on public repositories)</h3>
<dl>

<dt>Install Jena's Virtuoso Driver</dt>
	<dd>https://stackoverflow.com/questions/41137342/is-there-any-usable-dependency-for-virtuoso-jena-driver</dd>
<dt>Maven</dt>
	<dd>https://maven.apache.org/download.cgi</dd>

<dt>Maven instructions</dt>
	<dd>https://www.mkyong.com/maven/how-to-install-maven-in-windows/</dd>

<dt>For the Virtuoso maven jar generation</dt>
	<dd>Do this (add quotes compared to the solution proposed, as it otherwise complains with some errors)</dd>
	<dd>Explained why here: https://stackoverflow.com/questions/16348459/error-the-goal-you-specified-requires-a-project-to-execute-but-there-is-no-pom</dd>

<dt>Commands</dt>
	<dd>mvn install:install-file -q "-Dfile=./virt_jena3.jar" "-DgroupId=com.openlink.virtuoso" "-DartifactId=virt_jena3" "-Dversion=3.0" "-Dpackaging=jar" "-DgeneratePom=true"</dd>
	<dd>mvn install:install-file -q "-Dfile=./virtjdbc4.jar" "-DgroupId=com.openlink.virtuoso" "-DartifactId=virtjdbc4" "-Dversion=4.0" "-Dpackaging=jar" "-DgeneratePom=true"</dd>

</dl>

<h2>Package Details</h2>
<dl>
<dt>*.debug</dt>
	<dd>Classes used for debugging / analysis</dd>
<dt>*.deprecated</dt>
	<dd>Classes that are deprecated for the current version, but might make a comeback depending on direction and progress of research.</dd>
<dt>alu.linking</dt>
	<dd>Main Project Folder</dd>
<dt>alu.linking.api</dt>
	<dd>Classes related to API calls (currently only NIF API for GERBIL)</dd>
<dt>alu.linking.candidategeneration</dt>
	<dd>Classes related to candidate generation</dd>
<dt>alu.linking.config</dt>
	<dd>Configuration-related classes and packages</dd>
<dt>alu.linking.config.constants</dt>
	<dd>Configuration-related runtime constants (e.g. file locations, knowledge graphs, server connections, ...)</dd>
<dt>alu.linking.config.kg</dt>
	<dd>Knowledge graph-related constants (e.g. supported KGs and related entity queries)</dd>
<dt>alu.linking.disambiguation</dt>
	<dd>Disambiguation-related classes and packages</dd>
<dt>alu.linking.disambiguation.hops</dt>
	<dd>Classes related to graph-hopping through KGs</dd>
<dt>alu.linking.disambiguation.hops.graph</dt>
	<dd>In-memory graph related classes</dd>
<dt>alu.linking.disambiguation.hops.pathbuilding</dt>
	<dd>Classes used for crawling paths within KGs (for 'hopping')</dd>
<dt>alu.linking.disambiguation.pagerank	<dd></dt>
	<dd>PageRank-related classes (Note: apriori PageRank computation can be found in eu.wdaqua.pagerank; contextual disambiguation 'Sub-PageRank' can be found in alu.linking.disambiguation.scorers.subpagerank)</dd>
<dt>alu.linking.disambiguation.scorers</dt>
	<dd>Scorers and scorer-related classes used for disambiguation</dd>
<dt>alu.linking.disambiguation.scorers.embedhelp</dt>
	<dd>Helper Classes used by various embeddings-related scorers</dd>
<dt>alu.linking.disambiguation.scorers.hillclimbing</dt>
	<dd>Choosing / 'Picking' Schemes related to Hill-Climbing</dd>
<dt>alu.linking.disambiguation.scorers.pairwise</dt>
	<dd>Choosing / 'Picking' Schemes related to pairwise strategies</dd>
<dt>alu.linking.disambiguation.scorers.subpagerank</dt>
	<dd>Choosing / 'Picking' Schemes related to contextual (aka. not apriori) versions of PageRank</dd>
<dt>alu.linking.executable</dt>
	<dd>All kinds of classes that can be executed through our Pipeline instance</dd>
<dt>alu.linking.executable.preprocessing</dt>
	<dd>Preprocessing-related executable classes</dd>
<dt>alu.linking.executable.preprocessing.loader</dt>
	<dd>Executable classes related to loading (precomputed) data (e.g. PageRank, Mention possibilities, ...)</dd>
<dt>alu.linking.executable.preprocessing.nounphrases</dt>
	<dd>Executable classes related to nounphrases and their extraction</dd>
<dt>alu.linking.executable.preprocessing.setup</dt>
	<dd>Executable preprocessing classes related to project setup (prior to proper linking pipeline execution)</dd>
<dt>alu.linking.executable.preprocessing.setup.surfaceform</dt>
	<dd>Executable preprocessing classes related to surface form</dd>
<dt>alu.linking.executable.preprocessing.setup.surfaceform.processing</dt>
	<dd>Executable classes related to surface forms' processing for proper use</dd>
<dt>alu.linking.executable.preprocessing.setup.surfaceform.processing.url</dt>
	<dd>Executable classes related to URL-based surface forms' processing</dd>
<dt>alu.linking.executable.preprocessing.setup.surfaceform.query</dt>
	<dd>Executable classes related to surface forms' acquisition through query executions (e.g. to a Jena or Virtuoso-loaded KG)</dd>
<dt>alu.linking.executable.preprocessing.util</dt>
	<dd>Executable classes mostly boiling down to utility classes for other executables or their related classes</dd>
<dt>alu.linking.launcher</dt>
	<dd>Various entry points to the code - either for some preprocessing steps, different kinds of disambiguation alternatives (e.g. via API, stdin, etc.)</dd>
<dt>alu.linking.mentiondetection</dt>
	<dd>Mention detection related classes (aka. detecting mentions from plaintext)</dd>
<dt>alu.linking.mentiondetection.exact</dt>
	<dd>Classes related to exact mention detection</dd>
<dt>alu.linking.mentiondetection.fuzzy</dt>
	<dd>Classes related to fuzzy mention detection</dd>
<dt>alu.linking.postprocessing</dt>
	<dd>Classes related to post processing of files</dd>
<dt>alu.linking.preprocessing</dt>
	<dd>Classes related to preprocessing (non-executables, but potential dependendencies for executable classes)</dd>
<dt>alu.linking.structure</dt>
	<dd>Mostly global interfaces or bean-type classes relating to code as well as execution structure</dd>
<dt>alu.linking.utils</dt>
	<dd>Contains largely singleton or static utility classes potentially used by a multitude of classes</dd>
<dt>de.dwslab.petar</dt>
	<dd>RDF2Vec-related classes (including modifications)</dd>
<dt>eu.wdaqua</dt>
	<dd>PageRank implementation-related classes</dd>
<dt>org.aksw.gerbil</dt>
	<dd>GERBIL evaluation framework-related classes (mostly used by alu.linking.api)</dd>
</dl>
</body>