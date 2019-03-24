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
# Constants
cbow = 0
skipgram = 1

# Path to a file that contains lines with the locations of files 
# containing the sentences we want for our Word2Vec model
pathsLocator = "./sentencesPaths.txt"
vocabPath = "./embeddings_vocabulary.txt"

# Machine-based parameters
threadCount = 40

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
iterations = 10
negSampling = 15
windowContextSize = 5

#What is the newline character on the machine
newline = '\n'
ignorePrefix = '#'
#What separates one walk from another (aka. one sentence from another)?
walkSeparator = "\t"
#What separates the single 'units' of a given walk?
hopSeparator = '->'
iterationCounter = {'val': 0}

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
                    if False:
                        # If you are GROUPING the paths and separating them by TABs
                        lineTokens = line.rstrip(newline).split(walkSeparator)
                        # No need to split on tab characters as paths are not grouped right now
                        #
                        for token in lineTokens:
                            sentence = token.split(hopSeparator)
                            yield sentence
                    else:
                        # If you're NOT grouping the walks and separating them by tabs
                        sentence = line.rstrip(newline).split(hopSeparator)
                        #print(sentence)
                        yield sentence
                        
            except Exception:
                print("Failed reading file:")
                print(sentencesPath)

# sg 500
#sentencez = grab_sentences()
model = Word2Vec(sentences=MySentences(iterationCounter), 
size=200, 
workers=threadCount, 
window=windowContextSize, 
sg=skipgram, 
negative=negSampling, 
iter=iterations)

#model.build_vocab(MySentences())
#model.train(MySentences())

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

# sg/cbow features iterations window negative hops random walks
model.save('MAG_sg1_size200_mincount1_window5_neg15_iter15')

if False:
    # sg 200
    model1 = Word2Vec(size=200, 
    workers=threadCount, 
    window=5, 
    sg=skipgram, 
    negative=negSampling, 
    iter=iterations)
    
    model1.reset_from(model)

    # cbow 500
    model2 = Word2Vec(size=500, 
    workers=threadCount, 
    window=windowContextSize, 
    sg=cbow, 
    iter=iterations, 
    cbow_mean=1, 
    alpha=0.05)
    
    model2.reset_from(model)

    # cbow 200
    model3 = Word2Vec(size=200, 
    workers=threadCount, 
    window=windowContextSize, 
    sg=cbow, 
    iter=iterations, 
    cbow_mean=1, 
    alpha=0.05)
    model3.reset_from(model)

    del model

    model1.train(sentences)
    model1.save('DB2Vec_sg_200_5_5_15_2_500')

    del model1

    model2.train(sentences)
    model2.save('DB2Vec_cbow_500_5_5_2_500')

    del model2

    model3.train(sentences)
    model3.save('DB2Vec_cbow_200_5_5_2_500')

    del model3
