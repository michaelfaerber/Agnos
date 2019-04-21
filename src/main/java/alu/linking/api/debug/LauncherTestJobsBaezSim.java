package alu.linking.api.debug;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import alu.linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import alu.linking.utils.EmbeddingsUtils;

public class LauncherTestJobsBaezSim {

	public static void main(String[] args) {
		//http://dbpedia.org/resource/Steve_Jobs
		final Number[] jobs = new Number[]{0.3163692, -0.024344042, 0.7913639, 0.74422896, -0.41533455, -0.37735373, -0.4656563, -0.091887094, -0.3499222, 0.087971285, -0.20827816, -0.6230313, -0.4510446, -0.022345588, -0.55074704, -0.034691464, -0.6532178, -0.043876585, -0.28302518, 0.13545452, 0.10851453, 0.60540754, -0.8015782, 0.3050616, -0.20813319, 0.15552568, 0.78755134, 0.36606684, -0.09459484, -0.99666977, -0.39570415, -0.15613827, -0.32888505, 0.5081638, 0.874516, -0.13624913, 0.29748538, -0.13866031, -0.088204384, -0.16060731, 0.8788394, -0.16437764, 0.26649484, 0.4934807, -0.053382907, -1.1801549, 0.26749602, 0.07604147, 0.14978479, 0.40656626, -0.0029584512, -0.33834484, -0.087248705, 0.47137406, -1.0163428, 0.4163682, -0.26208538, 0.7085163, 0.1486754, -0.42058402, -0.81002253, 0.005704108, 0.32668513, 0.20310901, -0.05653235, -0.5209133, 1.0389245, -0.5260649, 0.36117235, -0.74713725, 0.5262007, 0.15583223, 0.14269054, 0.27895504, 0.6739978, 0.60993844, -0.46978125, -0.124721944, -0.5019103, -0.4847021, 0.88073033, -0.44401312, -0.20340422, -0.12994307, 0.48039126, 0.03732895, 1.0116538, -0.4367172, 0.18373187, 0.36825514, -0.25151286, -0.13520062, -0.5352804, -0.25466335, 0.028656647, 0.26623634, -0.44605654, -0.0061939424, 0.0067202635, 1.3256915, 0.89842933, -0.45531455, 0.2945282, 0.42623806, -0.32160392, 0.18646139, 0.14237836, 0.64297485, -0.70522296, -0.1308094, -0.5439532, -0.16935644, -0.2794558, -0.58375895, 0.72770846, -0.4748704, 0.2751253, 0.30456564, -0.85237736, 0.39856943, 0.060577102, 1.1097383, -0.11308476, -0.12851578, -0.15486047, -0.32823008, 0.5200613, -0.07279494, 0.18259975, -0.088338636, 0.3821998, 0.2554817, -0.12747142, 0.09661117, -0.19844994, -0.1498898, -0.020829152, -0.34603274, 1.0322886, 0.09042644, 0.4412628, 0.5417059, 0.003546235, 0.40542924, 0.6619408, -0.46385214, 0.2128758, -0.18853831, -0.31386366, -0.4736399, 0.6304477, 0.27600938, -0.089853644, 0.7772287, -0.25411484, -0.047103576, -0.12471702, -0.44181716, 0.17266005, -0.63163644, 0.545268, -1.2590032, -0.26938465, 0.80228555, 0.2508931, -0.36299816, -0.33694646, -0.37544686, 0.093513206, -0.0398877, -0.06808337, 0.40412986, -0.4054846, -0.45507702, -0.27305102, 0.30647227, -0.34540433, 0.028459787, 0.2278502, -0.43340552, -0.14468998, -0.24155414, 0.33304742, 0.041301694, -0.3919915, -0.47338447, 0.14463112, -0.08693997, -0.04392435, 0.4291072, 0.6749319, 0.16059385, -0.3302771, -0.60219854, -0.070631266, -0.60044754, 0.40703773, 0.51918346, -0.10778025, -1.2095665};
		//http://dbpedia.org/resource/Joan_Baez
		final Number[] baez = new Number[]{ 0.25418574, -0.13852304, 0.3045751, 0.075058624, -0.238455, 0.13598752, 0.12576394, -0.16312568, 0.22290671, 0.5031465, -0.12764509, 0.44585556, -0.18346646, -0.409359, -0.41567, 0.34198108, -0.9518316, -0.44755667, -0.021003252, -0.17457786, -0.62503624, 1.2942848, -1.0432729, 0.18439667, 0.17919491, -0.14624268, 1.0215113, -0.1753775, -0.12437307, -0.27968365, 0.36276165, -0.027696878, -0.06837825, 0.63659906, 0.8998831, 0.29822314, 0.28902224, -0.19236949, -0.6358075, -0.5211604, 1.2694083, -0.0902679, 0.031581853, 0.4444919, 0.13346134, -0.5398081, -0.07872843, -0.09922759, 0.06659545, 1.0236584, -0.06456567, -0.59993994, 0.097057596, 0.24729072, 0.122230396, -0.04019125, -0.31109747, -0.78220123, -0.30907843, 0.2380949, -0.03263224, 0.19089301, 0.4947057, -0.24570458, -0.5513764, 0.46316615, -0.20997605, -0.029833313, -0.08018371, -0.26758432, 0.41030487, 0.45107287, 0.31641957, 0.17462046, -0.23168784, 0.020950751, 0.29445913, -0.2381662, 0.43304813, -0.6951554, -0.265737, -0.57647735, 0.37539214, -0.84461075, 0.13880529, 0.61541873, 0.5241519, 0.10073959, -0.23108281, 0.10853903, -0.95570827, -0.3253879, -0.87206644, -0.28849122, -0.28387478, 0.16745558, 0.18662475, -0.20798483, -0.2801694, 1.3751552, -0.10852762, -0.26923546, -0.44309527, 0.6862383, -0.009168665, 0.28179953, 0.80902797, -0.38536114, 0.35573572, 0.2578044, -0.30182028, -0.5488516, 0.1242641, -0.28369433, 0.057546545, 0.25161284, -0.3981039, 0.31597498, 0.0023451853, 0.58310413, -0.020070514, 0.7186546, -0.37214112, 0.8313041, -0.20962538, -0.389647, -0.18438752, -0.343691, 0.10721306, -0.621572, -0.0037470246, -0.3732129, 0.307611, 0.30121246, 0.21444252, -0.76431423, 0.368405, -0.24278937, 0.7173672, 0.39389008, -0.22893845, -0.04070238, -0.24403375, -0.10669805, 0.75812966, -0.21007396, -0.005953069, -0.4759342, -0.45781368, -0.33027312, 0.24244177, 0.38831535, -0.1843101, 0.5743351, -0.4939347, 0.008210754, -0.35198247, 0.26939613, 0.5626812, -0.2777167, 0.28771222, -0.479009, -0.17001913, 0.165858, -0.42784628, 0.31825495, -0.18254876, 0.11548502, 0.22443904, 0.7010553, 0.02067506, -0.1047911, 0.3815965, -1.3344574, -0.77454174, 0.6160731, -0.026200727, -0.29134104, 0.21804784, 0.81434035, -0.41426146, -0.1698754, 0.059656482, -0.11799753, -0.3061025, 0.12149887, 0.41646406, 0.67691606, 0.52100146, 0.13396706, 0.404222, 0.16135173, -0.1990858, -0.38902506, 0.035232063, -0.02375283, 0.03296181, 0.3860629, -0.47257876, -0.7331222};
		final Number[] jobsDubai = new Number[]{0.61559725, -0.18817295, 0.19332086, 0.21517469, 0.001759226, -0.28299174, 0.16557446, 0.4346234, -0.2953175, -0.074188076, -0.35474378, -0.34182674, -0.2574721, -0.6443518, 0.35647815, 0.046124365, 0.0077378685, 0.11979875, -0.015636126, 0.32189572, 0.30560368, 0.13268125, -0.4947345, 0.12175231, 0.4150049, 0.20362526, 0.24315865, 0.23332706, 0.526854, -0.344716, 0.2070871, 0.29002, -0.28629407, 0.059073593, 0.65595746, 0.32264107, 0.40121043, -0.14371726, 0.82135314, -0.12300441, -0.21711421, 0.013646067, -0.023291007, 0.47007963, 0.34391674, -0.22984129, 1.0845016, -0.15270971, 0.38786465, 0.30825943, 0.022766346, 0.22507487, 0.14020598, 0.2357502, 0.42353517, 0.2362322, -0.051360924, 0.46408927, -0.29178032, -0.99791706, -0.109587006, 0.4427991, 0.022248438, 0.5380298, -0.16625148, -0.08374981, 0.116206765, 0.9143177, 0.2925162, 0.865187, -0.97749937, -0.0030347856, -0.23624249, 0.41586068, 0.100668214, 0.23244332, -0.4519118, -0.4512998, 0.4562073, 0.30963963, -0.13669163, 0.22638999, -0.50355643, -0.29583678, 0.34543917, 0.09167457, -0.39489993, 0.13321315, -0.28528604, 0.13638264, -0.6372895, 0.2912819, -0.101266496, -0.19711393, 0.32533866, -0.16212499, -1.1240232, -0.13000263, 0.15807395, 0.4913365, 0.36391306, -0.35053492, -0.60506994, 0.29361364, -0.09373046, 0.007384551, 0.19224137, 0.56501734, 0.113598645, -0.0063974857, 0.030548977, -0.39397725, 0.022632154, -0.0979605, 0.15115446, 0.21242553, -0.49383253, 0.21387404, -0.35057285, 0.009222613, -0.116059475, 0.12980662, 0.25420582, 0.39666808, -0.13569213, 0.60104483, 1.1973114, 0.1647849, 1.0142943, 0.274096, 0.18537413, 0.37483373, 0.27222067, -0.3172924, -0.30551407, 0.1306785, 0.45913845, 0.3459497, 0.0019240654, -0.056964763, -0.74112594, 0.17505693, 0.10508574, 0.18405274, 0.31262058, -0.41097364, 0.17279367, -0.63664085, -0.071205705, -0.2684093, -0.082020685, 0.3960565, -0.56584066, 0.11361697, 0.23536474, 0.4660265, 0.20668435, -0.1503173, -0.08310792, -0.15704831, 0.14182895, -0.8347195, -0.20956299, 0.5751186, -0.24210007, 0.23033033, 0.16040179, 0.21116555, 0.2631999, 0.032283712, 0.11841434, -0.49513242, -0.17706347, -0.5139732, -0.031776983, 0.16883372, -0.49889523, -0.2594534, -0.3381056, 0.15606627, -0.2346247, 0.13211146, 0.3189306, 0.41269147, -0.021337792, 0.65430474, 0.054555286, 0.0377925, 0.016212404, 0.18843243, 0.57122827, 0.018516917, 0.22718392, -0.37376654, -0.39297938, 0.07765694, -0.01985792, 0.101067185, 0.17797752, 0.027987156};
		Map<String, List<Number>> embeddings = new HashMap<>();
		final String baezStr = "http://dbpedia.org/resource/Joan_Baez";
		final String jobsStr = "http://dbpedia.org/resource/Steve_Jobs";
		final String jobsDubaiStr = "http://dbpedia.org/resource/Jobs_in_Dubai";
		embeddings.put(jobsStr, Arrays.asList(jobs));
		embeddings.put(baezStr, Arrays.asList(baez));
		embeddings.put(jobsDubaiStr, Arrays.asList(jobsDubai));
		EntitySimilarityService simService = new EntitySimilarityService(embeddings);
		System.out.println("Jobs("+jobs.length+")");
		System.out.println("Baez("+baez.length+")");
		System.out.println("Sim SJ-JB:"+EmbeddingsUtils.cosineSimilarity(Arrays.asList(jobs), Arrays.asList(baez)));
		System.out.println("Sim JB-JD:"+EmbeddingsUtils.cosineSimilarity(Arrays.asList(baez), Arrays.asList(jobsDubai)));
		System.out.println("Sim SJ-JD:"+EmbeddingsUtils.cosineSimilarity(Arrays.asList(jobs), Arrays.asList(jobsDubai)));
		System.out.println(simService.topSimilarity(jobsStr, Arrays.asList(new String[] {baezStr, jobsDubaiStr}), false));
	}
}
