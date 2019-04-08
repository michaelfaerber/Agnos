'''
Created on Feb 16, 2016
Updated November 2018
@author: petar
@author: kris
'''
# import modules; set up logging
from gensim.models import Word2Vec
import logging, os, sys, gzip
import datetime

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', filename='word2vec.out', level=logging.INFO)
# Which setup models to train
loadFirst = False
loadFirstLocation = "./dbpedia_sg1_size200_mincount1_window5_neg15_iter10"
trainFirst = True
trainSecond = False
trainThird = False
trainFourth = False

# Output variables
# Whether to output the full vocabulary
outputFullVocab = False
# How to save the models
kg = "DBpedia"

# If another model is in memory/loaded, put it here after training
loadedModel = None

# Constants
cbow = 0
skipgram = 1

# Mapping dict
entity_mapping_dict = {}
# Mapping file
mapping_file = "/home/noulletk/prog/bmw/dbpedia_full/resources/data/walks/walk_entity_mapping.txt"
mapping_sep = "\t"
hasMapping = False

# Path to a file that contains lines with the locations of files 
# containing the sentences we want for our Word2Vec model
pathsLocator = "./sentencesPaths.txt"
vocabPath = "./embeddings_vocabulary.txt"

# W2V Defaults ( https://radimrehurek.com/gensim/models/word2vec.html )
# gensim.models.word2vec.Word2Vec(
# sentences=None, corpus_file=None, size=100, alpha=0.025, 
# window=5, min_count=5, max_vocab_size=None, sample=0.001, 
# seed=1, workers=3, min_alpha=0.0001, sg=0, hs=0, negative=5, 
# ns_exponent=0.75, cbow_mean=1, hashfxn=<built-in function hash>, 
# iter=5, null_word=0, trim_rule=None, sorted_vocab=1, 
# batch_words=10000, compute_loss=False, callbacks=(), 
# max_final_vocab=None
# )

# W2V Hyperparameters
negSampling = 15
windowContextSize = 5
iterations = 5
minCount = 5
# Machine-based parameters
threadCount = 60

#What is the newline character on the machine
newline = '\n'
ignorePrefix = '#'
#What separates one walk from another (aka. one sentence from another)?
walkSeparator = "\t"
#What separates the single 'units' of a given walk?
hopSeparator = '->'
iterationCounter = {'val': 0}

#Load mappings if there are any
if hasMapping:
	for mapping_line in open(mapping_file, mode='rt'):
		mapping_tokens = mapping_line.rstrip(newline).split(mapping_sep)
		if len(mapping_tokens) == 2:
			entity_mapping_dict[mapping_tokens[0]] = mapping_tokens[1]
	print("Loaded %s mappings!" % (len(entity_mapping_dict)))


def outputVocab(outPath='embeddings_vocabulary', model = None):
	outFile = open(vocabPath, "w")
	print("Vocab keys size:",len(model.wv.vocab.keys()))
	print("Outputting vocab to: ",vocabPath)
	for key in model.wv.vocab.keys():
		outFile.write("%s" % key)
		for item in model.wv[key]:
			outFile.write("\t%s" % item)
		#outFile.write("\t%s" % value)
		outFile.write("\n")
	outFile.close()
	print("Finished outputting vocabulary")
	#print("Vocabulary keys:",model.wv.vocab.keys())
	#print("Vocab size:",len(model.wv.vocab))
	print("Vocab keys size:",len(model.wv.vocab.keys()))
	#print("Embedding: ",model.wv["Service-->"])
	#print("Most similar: ",model.most_similar(find_most_similar_to))
	#print("PASSED!")


def concatModelPath(kg='', sg='', size='', minCount='', window='', negSampling='', iterations='', alpha='', cbow_mean='') -> str:
	outPath = kg + "_sg" + str(skipgram) + "_size" + str(size) + "_minCount" + str(minCount) + "_window" + str(windowContextSize) \
	+ "_neg" + str(negSampling) + "_iter" + str(iterations)

	print("Outpath: %s" % (outPath) )
	#return 'DB2Vec_sg_200_5_5_15_2_500'
	return outPath


def trainOrLoad(loadedModel=None, kg=kg, vectSize=200, minCt=minCount, windowSize=windowContextSize, workerCt=threadCount, 
	sgOrCBOW=skipgram, neg=negSampling, cbow_mean=1, alpha=0.025, iters=iterations, save=True) -> Word2Vec:
	iterationCounter['val'] = 0
	currModel = None

	# Concatenating here to make sure that we don't waste computation time for it to fail at it
	currModelOutPath = concatModelPath(kg=kg, sg=sgOrCBOW, size=vectSize, minCount=minCt, window=windowSize, negSampling=neg, iterations=iters, alpha=alpha, cbow_mean=cbow_mean)

	if loadedModel is not None:
		print("Training model based on passed model")
		currModel = Word2Vec(
		size=vectSize, 
		workers=workerCt, 
		min_count=minCt,
		window=windowSize, 
		sg=sgOrCBOW, 
		negative=neg, 
		iter=iters,
		alpha=alpha,
		cbow_mean=cbow_mean
		)
		currModel.reset_from(loadedModel)
		currModel.train(MySentences(iterationCounter), epochs=currModel.iter, total_examples=currModel.corpus_count)

	else:
		print("Training model from scratch")
		currModel = Word2Vec(
		sentences=MySentences(iterationCounter),
		size=vectSize, 
		min_count=minCt,
		workers=workerCt, 
		window=windowSize, 
		sg=sgOrCBOW, 
		negative=neg, 
		iter=iters,
		alpha=alpha,
		cbow_mean=cbow_mean
		)

	if save:
		currModel.save(currModelOutPath)

	return currModel


class MySentences:
	def __init__(self, iterationCounter):
		self.iterationCounter = iterationCounter

	def __iter__(self):
		print("Running Iteration #%s" % (iterationCounter['val']))
		iterationCounter['val'] += 1
		# Iterate to find which files are to be read
		for fname in open(pathsLocator, mode='rt'):  # os.listdir(self.dirname):
			sentencesPath = fname.rstrip(newline)
			# Ignore commented-out lines
			if sentencesPath.startswith(ignorePrefix):
				continue
			now = datetime.datetime.now()
			print("[%s] Grabbing sentences from: %s" % (now.strftime("%Y-%m-%d %H:%M"), sentencesPath))
			try:
				# Go through all paths
				for line in open(sentencesPath, mode='rt'):
					# If you're NOT grouping the walks and separating them by tabs
					sentence = line.rstrip(newline).split(hopSeparator)
					for tokenPos in range(len(sentence)):
						token = sentence[tokenPos]
						# Give the proper URL for the entity IF it exists, otherwise return the entity itself
						sentence[tokenPos] = entity_mapping_dict.get(token, token)
					#print(sentence)
					yield sentence

			except Exception:
				print("Failed reading file:")
				print(sentencesPath)

# sg 500
#sentencez = grab_sentences()


if trainFirst:
	firstSize = 128
	print("Training first model (from scratch)")
	currModelOutPath = concatModelPath(kg=kg, sg=skipgram, size=firstSize, minCount=minCount, window=windowContextSize, negSampling=negSampling, iterations=iterations)
	model = Word2Vec(sentences=MySentences(iterationCounter), 
	size=firstSize, 
	min_count=minCount,
	workers=threadCount, 
	window=windowContextSize, 
	sg=skipgram, 
	negative=negSampling, 
	iter=iterations)

	#model.build_vocab(MySentences())
	#model.train(MySentences())

	# sg/cbow features iterations window negative hops random walks
	model.save(currModelOutPath)
	loadedModel = model
else:
	print("Not training first model")


if loadFirst:
	print("Loading first model...")
	print("Loading model from: ",loadFirstLocation)
	loadedModel = Word2Vec.load(loadFirstLocation)


if trainSecond:
	print("Training model #2")
	model2 = trainOrLoad(loadedModel=loadedModel, 
	vectSize=200, 
	sgOrCBOW=skipgram, 
	save=True)	
	
	if loadedModel is None:
		loadedModel = model2
	

if trainThird:
	print("Training model #3")
	model3 = trainOrLoad(loadedModel=loadedModel,
		vectSize=500, 
		sgOrCBOW=cbow, 
		save=True,
		cbow_mean=1, 
		alpha=0.05)
		
	if loadedModel is None:
		loadedModel = model3


if trainFourth:
	print("Training model #4")
	model4 = trainOrLoad(loadedModel=loadedModel, 
	vectSize=200, 
	sgOrCBOW=cbow, 
	cbow_mean=1, 
	alpha=0.05,
	save=True)	

	if loadedModel is None:
		loadedModel = model4


if outputFullVocab:
	outputVocab(outPath=vocabPath, model=loadedModel)
else:
	print("Not outputting vocab")


print("Successfully finished training!")
#del model

#model1.train(sentences)
#model1.save('DB2Vec_sg_200_5_5_15_2_500')

#del model1

#model2.train(sentences)
#model2.save('DB2Vec_cbow_500_5_5_2_500')

#del model2

#model3.train(sentences)
#model3.save('DB2Vec_cbow_200_5_5_2_500')

#del model3
