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

*.debug</br>
	Classes used for debugging / analysis</br>
*.deprecated</br>
	Classes that are deprecated for the current version, but might make a comeback depending on direction and progress of research.</br>
</br>
alu.linking</br>
	Main Project Folder</br>
alu.linking.api</br>
	Classes related to API calls (currently only NIF API for GERBIL)</br>
alu.linking.candidategeneration</br>
	Classes related to candidate generation</br>
alu.linking.config</br>
	Configuration-related classes and packages</br>
alu.linking.config.constants</br>
	Configuration-related runtime constants (e.g. file locations, knowledge graphs, server connections, ...)</br>
alu.linking.config.kg</br>
	Knowledge graph-related constants (e.g. supported KGs and related entity queries)</br>
alu.linking.disambiguation</br>
	Disambiguation-related classes and packages</br>
alu.linking.disambiguation.hops</br>
	Classes related to graph-hopping through KGs</br>
alu.linking.disambiguation.hops.graph</br>
	In-memory graph related classes</br>
alu.linking.disambiguation.hops.pathbuilding</br>
	Classes used for crawling paths within KGs (for 'hopping')</br>
alu.linking.disambiguation.pagerank	</br>
	PageRank-related classes (Note: apriori PageRank computation can be found in eu.wdaqua.pagerank; contextual disambiguation 'Sub-PageRank' can be found in alu.linking.disambiguation.scorers.subpagerank)</br>
alu.linking.disambiguation.scorers</br>
	Scorers and scorer-related classes used for disambiguation</br>
alu.linking.disambiguation.scorers.embedhelp</br>
	Helper Classes used by various embeddings-related scorers</br>
alu.linking.disambiguation.scorers.hillclimbing</br>
	Choosing / 'Picking' Schemes related to Hill-Climbing</br>
alu.linking.disambiguation.scorers.pairwise</br>
	Choosing / 'Picking' Schemes related to pairwise strategies</br>
alu.linking.disambiguation.scorers.subpagerank</br>
	Choosing / 'Picking' Schemes related to contextual (aka. not apriori) versions of PageRank</br>
alu.linking.executable</br>
	All kinds of classes that can be executed through our Pipeline instance</br>
alu.linking.executable.preprocessing</br>
	Preprocessing-related executable classes</br>
alu.linking.executable.preprocessing.loader</br>
	Executable classes related to loading (precomputed) data (e.g. PageRank, Mention possibilities, ...)</br>
alu.linking.executable.preprocessing.nounphrases</br>
	Exeutable classes related to nounphrases and their extraction</br>
alu.linking.executable.preprocessing.setup</br>
	Executable preprocessing classes related to project setup (prior to proper linking pipeline execution)</br>
alu.linking.executable.preprocessing.setup.surfaceform</br>
	Executable preprocessing classes related to surface form</br>
alu.linking.executable.preprocessing.setup.surfaceform.processing</br>
	Executable classes related to surface forms' processing for proper use</br>
alu.linking.executable.preprocessing.setup.surfaceform.processing.url</br>
	Executable classes related to URL-based surface forms' processing</br>
alu.linking.executable.preprocessing.setup.surfaceform.query</br>
	Executable classes related to surface forms' acquisition through query executions (e.g. to a Jena or Virtuoso-loaded KG)</br>
alu.linking.executable.preprocessing.util</br>
	Executable classes mostly boiling down to utility classes for other executables or their related classes</br>
alu.linking.launcher</br>
	Various entry points to the code - either for some preprocessing steps, different kinds of disambiguation alternatives (e.g. via API, stdin, etc.)</br>
alu.linking.mentiondetection</br>
	Mention detection related classes (aka. detecting mentions from plaintext)</br>
alu.linking.mentiondetection.exact</br>
	Classes related to exact mention detection</br>
alu.linking.mentiondetection.fuzzy</br>
	Classes related to fuzzy mention detection</br>
alu.linking.postprocessing</br>
	Classes related to post processing of files</br>
alu.linking.preprocessing</br>
	Classes related to preprocessing (non-executables, but potential dependendencies for executable classes)</br>
alu.linking.structure</br>
	Mostly global interfaces or bean-type classes relating to code as well as execution structure</br>
alu.linking.utils</br>
	Contains largely singleton or static utility classes potentially used by a multitude of classes</br>
de.dwslab.petar</br>
	RDF2Vec-related classes (including modifications)</br>
eu.wdaqua</br>
	PageRank implementation-related classes</br>
org.aksw.gerbil</br>
	GERBIL evaluation framework-related classes (mostly used by alu.linking.api)</br>
</body>