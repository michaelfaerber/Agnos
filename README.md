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

<h2>Package information</h2>:
*.debug</b></br>
	Classes used for debugging / analysis</br>
*.deprecated</br>
	Classes that are deprecated for the current version, but might make a comeback depending on direction and progress of research.</br>
</br>
<b>alu.linking</b></br>
	Main Project Folder</br>
<b>alu.linking.api</b></br>
	Classes related to API calls (currently only NIF API for GERBIL)</br>
<b>alu.linking.candidategeneration</b></br>
	Classes related to candidate generation</br>
<b>alu.linking.config</b></br>
	Configuration-related classes and packages</br>
<b>alu.linking.config.constants</b></br>
	Configuration-related runtime constants (e.g. file locations, knowledge graphs, server connections, ...)</br>
<b>alu.linking.config.kg</b></br>
	Knowledge graph-related constants (e.g. supported KGs and related entity queries)</br>
<b>alu.linking.disambiguation</b></br>
	Disambiguation-related classes and packages</br>
<b>alu.linking.disambiguation.hops</b></br>
	Classes related to graph-hopping through KGs</br>
<b>alu.linking.disambiguation.hops.graph</b></br>
	In-memory graph related classes</br>
<b>alu.linking.disambiguation.hops.pathbuilding</b></br>
	Classes used for crawling paths within KGs (for 'hopping')</br>
<b>alu.linking.disambiguation.pagerank	</b></br>
	PageRank-related classes (Note: apriori PageRank computation can be found in eu.wdaqua.pagerank; contextual disambiguation 'Sub-PageRank' can be found in alu.linking.disambiguation.scorers.subpagerank)</br>
<b>alu.linking.disambiguation.scorers</b></br>
	Scorers and scorer-related classes used for disambiguation</br>
<b>alu.linking.disambiguation.scorers.embedhelp</b></br>
	Helper Classes used by various embeddings-related scorers</br>
<b>alu.linking.disambiguation.scorers.hillclimbing</b></br>
	Choosing / 'Picking' Schemes related to Hill-Climbing</br>
<b>alu.linking.disambiguation.scorers.pairwise</b></br>
	Choosing / 'Picking' Schemes related to pairwise strategies</br>
<b>alu.linking.disambiguation.scorers.subpagerank</b></br>
	Choosing / 'Picking' Schemes related to contextual (aka. not apriori) versions of PageRank</br>
<b>alu.linking.executable</b></br>
	All kinds of classes that can be executed through our Pipeline instance</br>
<b>alu.linking.executable.preprocessing</b></br>
	Preprocessing-related executable classes</br>
<b>alu.linking.executable.preprocessing.loader</b></br>
	Executable classes related to loading (precomputed) data (e.g. PageRank, Mention possibilities, ...)</br>
<b>alu.linking.executable.preprocessing.nounphrases</b></br>
	Exeutable classes related to nounphrases and their extraction</br>
<b>alu.linking.executable.preprocessing.setup</b></br>
	Executable preprocessing classes related to project setup (prior to proper linking pipeline execution)</br>
<b>alu.linking.executable.preprocessing.setup.surfaceform</b></br>
	Executable preprocessing classes related to surface form</br>
<b>alu.linking.executable.preprocessing.setup.surfaceform.processing</b></br>
	Executable classes related to surface forms' processing for proper use</br>
<b>alu.linking.executable.preprocessing.setup.surfaceform.processing.url</b></br>
	Executable classes related to URL-based surface forms' processing</br>
<b>alu.linking.executable.preprocessing.setup.surfaceform.query</b></br>
	Executable classes related to surface forms' acquisition through query executions (e.g. to a Jena or Virtuoso-loaded KG)</br>
<b>alu.linking.executable.preprocessing.util</b></br>
	Executable classes mostly boiling down to utility classes for other executables or their related classes</br>
<b>alu.linking.launcher</b></br>
	Various entry points to the code - either for some preprocessing steps, different kinds of disambiguation alternatives (e.g. via API, stdin, etc.)</br>
<b>alu.linking.mentiondetection</b></br>
	Mention detection related classes (aka. detecting mentions from plaintext)</br>
<b>alu.linking.mentiondetection.exact</b></br>
	Classes related to exact mention detection</br>
<b>alu.linking.mentiondetection.fuzzy</b></br>
	Classes related to fuzzy mention detection</br>
<b>alu.linking.postprocessing</b></br>
	Classes related to post processing of files</br>
<b>alu.linking.preprocessing</b></br>
	Classes related to preprocessing (non-executables, but potential dependendencies for executable classes)</br>
<b>alu.linking.structure</b></br>
	Mostly global interfaces or bean-type classes relating to code as well as execution structure</br>
<b>alu.linking.utils</b></br>
	Contains largely singleton or static utility classes potentially used by a multitude of classes</br>
<b>de.dwslab.petar</b></br>
	RDF2Vec-related classes (including modifications)</br>
<b>eu.wdaqua</b></br>
	PageRank implementation-related classes</br>
<b>org.aksw.gerbil</b></br>
	GERBIL evaluation framework-related classes (mostly used by alu.linking.api)</br>
</body>