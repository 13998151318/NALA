from warnings import simplefilter
simplefilter(action='ignore', category=FutureWarning)
import logging
logging.getLogger("transformers.tokenization_utils").setLevel(logging.ERROR)
logging.basicConfig(level=logging.ERROR)
import torch
import torch.nn as nn
import torch.nn.functional as F
import random
import numpy as np
import pickle
import time
from Param import *
from utils import fixed,cos_sim_mat_generate,batch_topk
from read_data_func import read_structure_datas
from Basic_Bert_Unit_model import Basic_Bert_Unit_model


def candidate_generate(ents1,ents2,ent_emb,candidate_topk = 50,bs = 32, cuda_num = 0):
    """
    return a dict, key = entity, value = candidates (likely to be aligned entities)
    """
    emb1 = np.array(ent_emb)[ ents1 ].tolist()
    emb2 = np.array(ent_emb)[ ents2 ].tolist()
    print("Test(get candidate) embedding shape:",np.array(emb1).shape,np.array(emb2).shape)
    print("get candidate by cosine similartity.")
    res_mat = cos_sim_mat_generate(emb1,emb2,bs,cuda_num=cuda_num)
    score,index = batch_topk(res_mat,bs,candidate_topk,largest=True,cuda_num=cuda_num)
    ent2candidates = dict()
    for i in range( len(index) ):
        e1 = ents1[i]
        e2_list = np.array(ents2)[index[i]].tolist()
        ent2candidates[e1] = e2_list
    return ent2candidates


def all_entity_pairs_gene(candidate_dict_list, ill_pair_list):
    #generate list of all candidate entity pairs.
    entity_pairs_list = []
    for candidate_dict in candidate_dict_list:
        for e1 in candidate_dict.keys():
            for e2 in candidate_dict[e1]:
                entity_pairs_list.append((e1, e2))
    for ill_pair in ill_pair_list:
        for e1, e2 in ill_pair:
            entity_pairs_list.append((e1, e2))
    entity_pairs_list = list(set(entity_pairs_list))
    print("entity_pair (e1,e2) num is: {}".format(len(entity_pairs_list)))
    return entity_pairs_list


def save_in_csv_format(My_Labels, name):
    np.savetxt(name + '.csv', My_Labels, delimiter = ',', encoding="utf-8")  #, fmt='%.10f'  , fmt='%d'


def entlist2emb(Model,entids,entid2data,cuda_num):
    """
    return basic bert unit output embedding of entities
    """
    batch_token_ids = []
    batch_mask_ids = []
    for eid in entids:
        temp_token_ids = entid2data[eid][0]
        temp_mask_ids = entid2data[eid][1]

        batch_token_ids.append(temp_token_ids)
        batch_mask_ids.append(temp_mask_ids)

    batch_token_ids = torch.LongTensor(batch_token_ids).cuda(cuda_num)
    batch_mask_ids = torch.FloatTensor(batch_mask_ids).cuda(cuda_num)

    batch_emb = Model(batch_token_ids,batch_mask_ids)
    del batch_token_ids
    del batch_mask_ids
    return batch_emb


def compute_sim(Model, entid_1, entid_2,entid2data,batch_size,context = ""):
    start_time = time.time()
    print(context)
    Model.eval()
    with torch.no_grad():
        ents_1 = entid_1
        ents_2 = entid_2

        emb1 = []
        for i in range(0,len(ents_1),batch_size):
            batch_ents_1 = ents_1[i: i+batch_size]
            batch_emb_1 = entlist2emb(Model,batch_ents_1,entid2data,CUDA_NUM).detach().cpu().tolist()
            emb1.extend(batch_emb_1)
            del batch_emb_1

        emb2 = []
        for i in range(0,len(ents_2),batch_size):
            batch_ents_2 = ents_2[i: i+batch_size]
            batch_emb_2 = entlist2emb(Model,batch_ents_2,entid2data,CUDA_NUM).detach().cpu().tolist()
            emb2.extend(batch_emb_2)
            del batch_emb_2

        print("Cosine similarity of basic bert unit embedding res:")
        res_mat = cos_sim_mat_generate(emb1,emb2,batch_size,cuda_num=CUDA_NUM)
        save_in_csv_format(res_mat, ENT_EMB_PATH[:-4]+"_res_mat") # >>(4.12GB).

    print("compute_sim using time: {:.3f}".format(time.time()-start_time))
    print("--------------------")


def main():
    print("----------------get entity embedding--------------------")
    cuda_num = CUDA_NUM
    batch_size = 256
    print("GPU NUM:",cuda_num)

    # load basic bert unit model
    bert_model_path = BASIC_BERT_UNIT_MODEL_SAVE_PATH + BASIC_BERT_UNIT_MODEL_SAVE_PREFIX + "model_epoch_" \
                      + str(LOAD_BASIC_BERT_UNIT_MODEL_EPOCH_NUM) + '.p'
    Model = Basic_Bert_Unit_model(768, BASIC_BERT_UNIT_MODEL_OUTPUT_DIM)
    Model.load_state_dict(torch.load(bert_model_path, map_location='cpu'))
    print("loading basic bert unit model from:  {}".format(bert_model_path))
    Model.eval()
    for name, v in Model.named_parameters():
        v.requires_grad = False
    Model = Model.cuda(cuda_num)


    # read other data from bert unit model(train ill/test ill/eid2data)
    # (These files were saved during the training of basic bert unit)
    bert_model_other_data_path = BASIC_BERT_UNIT_MODEL_SAVE_PATH + BASIC_BERT_UNIT_MODEL_SAVE_PREFIX + 'other_data.pkl'
    train_ill, test_ill, eid2data = pickle.load(open(bert_model_other_data_path, "rb"))

    ent_ill, index2rel, index2entity, \
    rel2index, entity2index, \
    rel_triples_1, rel_triples_2, entid_1, entid_2 = read_structure_datas(DATA_PATH)
    print("train_ill num: {} /test_ill num: {} / train_ill & test_ill num: {}".format(len(train_ill),len(test_ill),
                                                                                 len(set(train_ill) & set(test_ill) )))

    #generate entity embedding by basic bert unit
    start_time = time.time()
    ent_emb = []
    bn = nn.BatchNorm1d(BASIC_BERT_UNIT_MODEL_OUTPUT_DIM, device=cuda_num)
    bn.train()
    for eid in range(0, len(eid2data.keys()), batch_size): #eid == [0,n)
        token_inputs = []
        mask_inputs = []
        for i in range(eid, min(eid + batch_size, len(eid2data.keys()))):
            token_input = eid2data[i][0]
            mask_input = eid2data[i][1]
            token_inputs.append(token_input)
            mask_inputs.append(mask_input)
        vec = Model(torch.LongTensor(token_inputs).cuda(cuda_num),
                    torch.FloatTensor(mask_inputs).cuda(cuda_num))
        vec = bn(vec)
    bn.eval()
    for eid in range(0, len(eid2data.keys()), batch_size): #eid == [0,n)
        token_inputs = []
        mask_inputs = []
        for i in range(eid, min(eid + batch_size, len(eid2data.keys()))):
            token_input = eid2data[i][0]
            mask_input = eid2data[i][1]
            token_inputs.append(token_input)
            mask_inputs.append(mask_input)
        vec = Model(torch.LongTensor(token_inputs).cuda(cuda_num),
                    torch.FloatTensor(mask_inputs).cuda(cuda_num))
        vec = bn(vec)
        vec = torch.nn.functional.normalize(vec)
        ent_emb.extend(vec.detach().cpu().tolist())
    print("get entity embedding using time {:.3f}".format(time.time() - start_time))
    print("entity embedding shape: ", np.array(ent_emb).shape)

    #compute_sim(Model, entid_1, entid_2, eid2data, BATCH_SIZE, context="save_sim_mat:")

    save_in_csv_format(ent_emb, ENT_EMB_PATH[:-4]) #284.19MB
    #save entity embedding.
    #pickle.dump(ent_emb, open(ENT_EMB_PATH, "wb"))
    print("save entity embedding....")

    entitynames = []
    for eid in range(0, len(eid2data.keys())):
        entitynames.append([index2entity[eid]])
    np.savetxt(ENT_EMB_PATH[:-4]+"_entity_names", entitynames, delimiter = ',',fmt="%s",encoding="utf-8")
    """
    entitynames1 = []
    for eid in entid_1:
        entitynames1.append([index2entity[eid]])
    np.savetxt(ENT_EMB_PATH[:-4]+"entity_names_1" + '.csv', entitynames1, delimiter = ',',fmt="%s",encoding="utf-8")
    entitynames2 = []
    for eid in entid_2:
        entitynames2.append([index2entity[eid]])
    np.savetxt(ENT_EMB_PATH[:-4]+"entity_names_2" + '.csv', entitynames2, delimiter = ',',fmt="%s",encoding="utf-8")
    """
    """
    #Generate candidates(likely to be aligned) for entities in train_set/test_set
    #we apply interaction model to infer a matching score on candidates.
    test_ids_1 = [e1 for e1, e2 in test_ill]
    test_ids_2 = [e2 for e1, e2 in test_ill]
    train_ids_1 = [e1 for e1, e2 in train_ill]
    train_ids_2 = [e2 for e1, e2 in train_ill]
    train_candidates = candidate_generate(train_ids_1, train_ids_2, ent_emb, CANDIDATE_NUM, bs=2048, cuda_num=CUDA_NUM)
    test_candidates = candidate_generate(test_ids_1, test_ids_2, ent_emb, CANDIDATE_NUM, bs=2048, cuda_num=CUDA_NUM)
    #save_in_csv_format(train_candidates, TRAIN_CANDIDATES_PATH[:-4])
    pickle.dump(train_candidates, open(TRAIN_CANDIDATES_PATH, "wb"))
    print("save candidates for training ILL data....")
    #save_in_csv_format(test_candidates, TEST_CANDIDATES_PATH[:-4])
    pickle.dump(test_candidates, open(TEST_CANDIDATES_PATH, "wb"))
    print("save candidates for testing ILL data....")

    #entity_pairs (entity_pairs is list of (likely to be aligned) entity pairs : [(e1,ea),(e1,eb),(e1,ec) ....])
    entity_pairs = all_entity_pairs_gene([ train_candidates,test_candidates ],[ train_ill ])
    pickle.dump(entity_pairs, open(ENT_PAIRS_PATH, "wb"))
    print("save entity_pairs save....")
    print("entity_pairs num: {}".format(len(entity_pairs)))
    """






if __name__ == '__main__':
    fixed(SEED_NUM)
    main()







