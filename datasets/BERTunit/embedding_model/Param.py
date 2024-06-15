"""
hyper-parameters:
"""
CUDA_NUM = 2 #GPU num
LANG = 'zh' #language 'zh'/'ja'/'fr'
ENTITY_NEIGH_MAX_NUM = 50 # max sampling neighbor num of entity
ENTITY_ATTVALUE_MAX_NUM = 50 #max sampling attributeValue num of entity
KERNEL_NUM = 21
SEED_NUM = 11037
CANDIDATE_NUM = 50 # candidate number

BATCH_SIZE = 128 # train batch size128
NEG_NUM = 5 # negative sampling num
LEARNING_RATE = 5e-4 # learning rate
MARGIN = 1 # margin
EPOCH_NUM = 200 # train epoch num

WITH_TRANS = False#True  False

INTERACTION_MODEL_SAVE_PATH = "../Save_model/interaction_model_{}en.bin".format(LANG) #interaction model save path.

#load model(base_bert_unit_model) path
excel_num = 194
BASIC_BERT_UNIT_MODEL_SAVE_PATH = "../Save_model/"
if WITH_TRANS:
    BASIC_BERT_UNIT_MODEL_SAVE_PREFIX = "trans_" + str(excel_num) + "_DBP15K_{}en".format(LANG)
else:
    BASIC_BERT_UNIT_MODEL_SAVE_PREFIX = str(excel_num) + "_DBP15K_{}en".format(LANG)

LOAD_BASIC_BERT_UNIT_MODEL_EPOCH_NUM = 14
BASIC_BERT_UNIT_MODEL_OUTPUT_DIM = 300

#load data path

DATA_PATH = r"../data/dbp15k/{}_en_2/".format(LANG)


#candidata_save_path
TRAIN_CANDIDATES_PATH = DATA_PATH + 'train_candidates.pkl'
TEST_CANDIDATES_PATH = DATA_PATH + 'test_candidates.pkl'

#entity embedding and attributeValue embedding save path.
ENT_EMB_PATH = DATA_PATH + '{}_emb_{}.pkl'.format(BASIC_BERT_UNIT_MODEL_SAVE_PREFIX,LOAD_BASIC_BERT_UNIT_MODEL_EPOCH_NUM)
ATTRIBUTEVALUE_EMB_PATH = DATA_PATH + str(excel_num) + "attribute_value_embedding.pkl"
ATTRIBUTEVALUE_LIST_PATH = DATA_PATH + str(excel_num) + "_attribute_value_list.pkl" #1-1 match to attributeValue embedding.
ATTRIBUTEVALUE_SIM_PATH = DATA_PATH + str(excel_num) + "_attribute_value_sim.pkl"

#(candidate) entity_pairs save path.
ENT_PAIRS_PATH = DATA_PATH + 'ent_pairs.pkl' #[(e1,ea),(e1,eb)...]

#interaction feature save filepath name
NEIGHBORVIEW_SIMILARITY_FEATURE_PATH = DATA_PATH + 'neighbor_view_similarity_feature.pkl' #1-1 match to entity_pairs
ATTRIBUTEVIEW_SIMILARITY_FEATURE_PATH = DATA_PATH + 'attribute_similarity_feature.pkl' #1-1 match to entity_pairs
DESVIEW_SIMILARITY_FEATURE_PATH = DATA_PATH + 'des_view_similarity_feature.pkl' #1-1 match to entity_pairs
