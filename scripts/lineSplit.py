from gensim.models import Word2Vec

sentences = [["cat", "say", "meow"], ["dog", "say", "woof"]]
model = Word2Vec(sentences=sentences, min_count=1, size=500, workers=60, window=10, sg=1, negative=15, iter=5)

#from gensim.models import Word2Vec
#sentences = [["cat", "say", "meow"], ["dog", "say", "woof"]]
#model = Word2Vec(sentences=sentences, min_count=1, window=10, size=500, workers=60, sg=1, iter=5, negative=15)
