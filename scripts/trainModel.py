'''
Created on Feb 16, 2016
Updated November 2018
@author: petar
@author: kris
'''
# import modules; set up logging
from gensim.models import Word2Vec
import logging, os, sys, gzip
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', filename='word2vec.out', level=logging.INFO)
# Path to a file that contains lines with the locations of files 
# containing the sentences we want for our Word2Vec model
pathsLocator = "./sentencesPaths.txt"
vocabPath = "./embeddings_vocabulary.txt"

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
                        yield sentence
#                        words.append(sentence)
#                    yield words
            except Exception:
                print("Failed reading file:")
                print(sentencesPath)

# sg 500
#sentencez = grab_sentences()
model = Word2Vec(sentences=MySentences(), min_count=1, size=500, workers=55, window=10, sg=1, negative=15, iter=2)
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
model.save('DB2Vec_sg_500_5_5_15_2_500')

# sg 200
model1 = Word2Vec(size=200, workers=5, window=5, sg=1, negative=15, iter=5)
model1.reset_from(model)

# cbow 500
model2 = Word2Vec(size=500, workers=5, window=5, sg=0, iter=5, cbow_mean=1, alpha=0.05)
model2.reset_from(model)

# cbow 200
model3 = Word2Vec(size=200, workers=5, window=5, sg=0, iter=5, cbow_mean=1, alpha=0.05)
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
