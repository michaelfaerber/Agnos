sentences = [['a', 'b', 'c'], ['a', 'd','e']]
def doThis(sentence):
	ret = None
	for x in sentence:
		if ret is None:
			ret = x
		else:
			ret += x
	return ret

default_val = ''
entity_embeddings_dict = {}
entity_embeddings_dict = {sentence[0]: doThis(sentence) + entity_embeddings_dict.get(sentence[0], default_val) \
for sentence in sentences }
print(entity_embeddings_dict)