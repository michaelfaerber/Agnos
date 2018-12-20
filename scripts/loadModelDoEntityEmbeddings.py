'''
@author: kris
'''
# import modules; set up logging
from gensim.models import Word2Vec
import numpy as np
import logging, os, sys, gzip
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', filename='word2vec.out', level=logging.INFO)
# Path to a file that contains lines with the locations of files 
# containing the sentences we want for our Word2Vec model
pathsLocator = "./sentencesPaths.txt"
outputPath = "./entity_embeddings.txt"
to_load = 'DB2Vec_sg_500_5_5_15_2_500'

class MySentences():
    def __iter__(self):
        for fname in open(pathsLocator, mode='rt'):  # os.listdir(self.dirname):
            sentencesPath = fname.rstrip('\n')
            print("Grabbing sentences from: %s" % sentencesPath)
            try:
                for line in open(sentencesPath, mode='rt'):
                    lineTokens = line.rstrip('\n').split("\t")
                    words = []
                    for token in lineTokens:
                        sentence = token.split('->')
                        #yield sentence
                        words.extend(sentence)
                    yield words
            except Exception:
                print("Failed reading file:")
                print(sentencesPath)

#load model
print("Loading model from: ",to_load)
model = Word2Vec.load(to_load)

print("Vocab keys size:",len(model.wv.vocab.keys()))
print("Outputting entity embeddings to: ",outputPath)
sentences = MySentences()
outFile = open(outputPath, "w")
for sentence in sentences:
    #print("sentence:",sentence)
    entity = sentence[0]
    entity_embedding = None
    #Sum over all words' embeddings and then output the resulting embedding
    for word in sentence:
        word_embedding = model.wv[word]
        if entity_embedding is None:
            entity_embedding = np.zeros(word_embedding.shape)
        entity_embedding += word_embedding
    #Output computed embedding
    outFile.write("%s" % entity)
    for number in entity_embedding:
        outFile.write("\t%s" % number)
    outFile.write("\n")
    
outFile.close()
print("Finished outputting entity embeddings")
