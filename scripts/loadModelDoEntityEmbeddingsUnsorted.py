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
to_load = 'RDF2Vec_sg1_size200_mincount1_window5_neg15_iter20'


#What is the newline character on the machine
newline = '\n'
#What separates one walk from another (aka. one sentence from another)?
walkSeparator = "\t"
#What separates the single 'units' of a given walk?
hopSeparator = '->'

class MySentences():
    def __iter__(self):
        for fname in open(pathsLocator, mode='rt'):  # os.listdir(self.dirname):
            sentencesPath = fname.rstrip(newline)
            print("Grabbing sentences from: %s" % sentencesPath)
            try:
                for line in open(sentencesPath, mode='rt'):
                    lineTokens = line.rstrip(newline).split(walkSeparator)
                    for token in lineTokens:
                        sentence = token.split(hopSeparator)
                        yield sentence
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

#Make a dictionary for in-memory aggregation while going over sentences
#Note: ONLY WORKS FOR LIMITED NUMBER OF ENTITIES, TRYING FOR 5KK NOW
default_val = None
entity_embeddings_dict = {}

print("Compute entity embeddings (through combination of word embeddings)...")
counter = 0

'''
for sentence in sentences:
    entity = sentence[0]
    entity_embedding = None
    #Sum over all words' embeddings and then output the resulting embedding
    for word in sentence:
        word_embedding = model.wv[word]
        if default_val is None:
            #Initialise default_val if it isn't yet
            default_val = np.zeros(word_embedding.shape)
        if entity_embedding is None:
            entity_embedding = np.zeros(word_embedding.shape)
        entity_embedding += word_embedding
    entity_embeddings_dict[entity] = entity_embeddings_dict.get(entity, default_val) + entity_embedding
    if (counter % 1000000 == 0):
        print("Combined word embeddings: ",counter)
        print("Last one completed: ",entity)
    counter+=1
'''
for sentence in sentences:
    # idea is that the entity is in the document, so we check what it is like and 
    # since every entity has 'the same' treatment, that we can determine their probabilities based on that
    entity = sentence[0]
    entity_embedding = None
    dict_val = entity_embeddings_dict.get(entity, None)
    if (dict_val is None):
        entity_embedding = model.wv[entity]
        entity_embeddings_dict[entity] = entity_embedding
    if (counter % 1000000 == 0):
        print("Combined word embeddings: ",counter)
        print("Last one completed: ",entity)
    counter+=1


print("Done w/ combining embeddings")
print("Output computed entity embeddings!")
for (entity, entity_embedding) in entity_embeddings_dict.items():
    #Output computed embedding
    outFile.write("%s" % entity)
    for number in entity_embedding:
        outFile.write("\t%s" % number)
    outFile.write("\n")

outFile.close()
print("Finished outputting entity embeddings")
