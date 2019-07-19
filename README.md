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
  
  
</table> 


<h2>Package information</h2>
<dl></br>
<dt>*.debug</dt>
	<dd>Classes used for debugging / analysis</dd></br>
<dt>*.deprecated</dt>
	<dd>Classes that are deprecated for the current version, but might make a comeback depending on direction and progress of research.</dd></br>
<dt>alu.linking</dt>
	<dd>Main Project Folder</dd></br>
<dt>alu.linking.api</dt>
	<dd>Classes related to API calls (currently only NIF API for GERBIL)</dd></br>
<dt>alu.linking.candidategeneration</dt>
	<dd>Classes related to candidate generation</dd></br>
<dt>alu.linking.config</dt>
	<dd>Configuration-related classes and packages</dd></br>
<dt>alu.linking.config.constants</dt>
	<dd>Configuration-related runtime constants (e.g. file locations, knowledge graphs, server connections, ...)</dd></br>
<dt>alu.linking.config.kg</dt>
	<dd>Knowledge graph-related constants (e.g. supported KGs and related entity queries)</dd></br>
<dt>alu.linking.disambiguation</dt>
	<dd>Disambiguation-related classes and packages</dd></br>
<dt>alu.linking.disambiguation.hops</dt>
	<dd>Classes related to graph-hopping through KGs</dd></br>
<dt>alu.linking.disambiguation.hops.graph</dt>
	<dd>In-memory graph related classes</dd></br>
<dt>alu.linking.disambiguation.hops.pathbuilding</dt>
	<dd>Classes used for crawling paths within KGs (for 'hopping')</dd></br>
<dt>alu.linking.disambiguation.pagerank	<dd></dt>
	<dd>PageRank-related classes (Note: apriori PageRank computation can be found in eu.wdaqua.pagerank; contextual disambiguation 'Sub-PageRank' can be found in alu.linking.disambiguation.scorers.subpagerank)</dd></br>
<dt>alu.linking.disambiguation.scorers</dt>
	<dd>Scorers and scorer-related classes used for disambiguation</dd></br>
<dt>alu.linking.disambiguation.scorers.embedhelp</dt>
	<dd>Helper Classes used by various embeddings-related scorers</dd></br>
<dt>alu.linking.disambiguation.scorers.hillclimbing</dt>
	<dd>Choosing / 'Picking' Schemes related to Hill-Climbing</dd></br>
<dt>alu.linking.disambiguation.scorers.pairwise</dt>
	<dd>Choosing / 'Picking' Schemes related to pairwise strategies</dd></br>
<dt>alu.linking.disambiguation.scorers.subpagerank</dt>
	<dd>Choosing / 'Picking' Schemes related to contextual (aka. not apriori) versions of PageRank</dd></br>
<dt>alu.linking.executable</dt>
	<dd>All kinds of classes that can be executed through our Pipeline instance</dd></br>
<dt>alu.linking.executable.preprocessing</dt>
	<dd>Preprocessing-related executable classes</dd></br>
<dt>alu.linking.executable.preprocessing.loader</dt>
	<dd>Executable classes related to loading (precomputed) data (e.g. PageRank, Mention possibilities, ...)</dd></br>
<dt>alu.linking.executable.preprocessing.nounphrases</dt>
	<dd>Executable classes related to nounphrases and their extraction</dd></br>
<dt>alu.linking.executable.preprocessing.setup</dt>
	<dd>Executable preprocessing classes related to project setup (prior to proper linking pipeline execution)</dd></br>
<dt>alu.linking.executable.preprocessing.setup.surfaceform</dt>
	<dd>Executable preprocessing classes related to surface form</dd></br>
<dt>alu.linking.executable.preprocessing.setup.surfaceform.processing</dt>
	<dd>Executable classes related to surface forms' processing for proper use</dd></br>
<dt>alu.linking.executable.preprocessing.setup.surfaceform.processing.url</dt>
	<dd>Executable classes related to URL-based surface forms' processing</dd></br>
<dt>alu.linking.executable.preprocessing.setup.surfaceform.query</dt>
	<dd>Executable classes related to surface forms' acquisition through query executions (e.g. to a Jena or Virtuoso-loaded KG)</dd></br>
<dt>alu.linking.executable.preprocessing.util</dt>
	<dd>Executable classes mostly boiling down to utility classes for other executables or their related classes</dd></br>
<dt>alu.linking.launcher</dt>
	<dd>Various entry points to the code - either for some preprocessing steps, different kinds of disambiguation alternatives (e.g. via API, stdin, etc.)</dd></br>
<dt>alu.linking.mentiondetection</dt>
	<dd>Mention detection related classes (aka. detecting mentions from plaintext)</dd></br>
<dt>alu.linking.mentiondetection.exact</dt>
	<dd>Classes related to exact mention detection</dd></br>
<dt>alu.linking.mentiondetection.fuzzy</dt>
	<dd>Classes related to fuzzy mention detection</dd></br>
<dt>alu.linking.postprocessing</dt>
	<dd>Classes related to post processing of files</dd></br>
<dt>alu.linking.preprocessing</dt>
	<dd>Classes related to preprocessing (non-executables, but potential dependendencies for executable classes)</dd></br>
<dt>alu.linking.structure</dt>
	<dd>Mostly global interfaces or bean-type classes relating to code as well as execution structure</dd></br>
<dt>alu.linking.utils</dt>
	<dd>Contains largely singleton or static utility classes potentially used by a multitude of classes</dd></br>
<dt>de.dwslab.petar</dt>
	<dd>RDF2Vec-related classes (including modifications)</dd></br>
<dt>eu.wdaqua</dt>
	<dd>PageRank implementation-related classes</dd></br>
<dt>org.aksw.gerbil</dt>
	<dd>GERBIL evaluation framework-related classes (mostly used by alu.linking.api)</dd>
</dl>

</body>