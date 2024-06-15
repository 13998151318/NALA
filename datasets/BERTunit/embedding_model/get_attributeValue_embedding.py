from warnings import simplefilter
simplefilter(action='ignore', category=FutureWarning)
import logging
logging.getLogger("transformers.tokenization_utils").setLevel(logging.ERROR)
logging.basicConfig(level=logging.ERROR)
import time
import os
import numpy as np
import pickle
from Param import *
from read_data_func import *
import torch
import torch.nn as nn
import torch.nn.functional as F
from transformers import BertTokenizer
from utils import fixed,cos_sim_mat_generate,batch_topk
from Basic_Bert_Unit_model import Basic_Bert_Unit_model
from tqdm import tqdm

def get_tokens_of_value(vaule_list,Tokenizer,max_length):
    #return tokens of attributeValue
    tokens_list = []
    for v in vaule_list:
        token_ids = Tokenizer.encode(v,add_special_tokens=True,max_length=max_length)
        tokens_list.append(token_ids)
    return tokens_list

def padding_to_longest(token_list,Tokenizer):
    #return padding attributeValue tokens and Masks
    max_length = max([len(tokens) for tokens in token_list])
    new_token_list = []
    for tokens in token_list:
        new_token_list.append(tokens + [Tokenizer.pad_token_id] * (max_length - len(tokens)))
    mask_list = np.ones(np.array(new_token_list).shape)
    mask_list[np.array(new_token_list) == Tokenizer.pad_token_id] = 0
    mask_list = mask_list.tolist()
    return torch.LongTensor(new_token_list),torch.FloatTensor(mask_list)


def attributeValue_emb_gene(l_set,Model,Tokenizer,batch_size,cuda_num,max_length):
    """
    generate attributeValue embedding by basic bert unit
    """
    all_l_emb = []
    start_time = time.time()
    bn = nn.BatchNorm1d(BASIC_BERT_UNIT_MODEL_OUTPUT_DIM, device=cuda_num)
    bn.train()
    for start_pos in range(0,len(l_set),batch_size):
        batch_l_list = l_set[start_pos : start_pos + batch_size]
        batch_token_list = get_tokens_of_value(batch_l_list,Tokenizer,max_length)
        tokens,masks = padding_to_longest(batch_token_list,Tokenizer)
        tokens = tokens.cuda(cuda_num)
        masks = masks.cuda(cuda_num)
        l_emb = Model(tokens,masks)
        l_emb = bn(l_emb)
    bn.eval()
    for start_pos in range(0,len(l_set),batch_size):
        batch_l_list = l_set[start_pos : start_pos + batch_size]
        batch_token_list = get_tokens_of_value(batch_l_list,Tokenizer,max_length)
        tokens,masks = padding_to_longest(batch_token_list,Tokenizer)
        tokens = tokens.cuda(cuda_num)
        masks = masks.cuda(cuda_num)
        l_emb = Model(tokens,masks) 
        l_emb = bn(l_emb)
        l_emb = torch.nn.functional.normalize(l_emb)
        l_emb = l_emb.detach().cpu().tolist()
        all_l_emb.extend(l_emb)
    
    print("attributeValue embedding generate using time {:.3f}".format(time.time()-start_time))
    assert len(all_l_emb) == len(l_set)
    return all_l_emb

def save_in_csv_format(My_Labels, name):
    np.savetxt(name + '.csv', My_Labels, delimiter = ',', fmt='%.10f')  #, fmt='%d'

def save_in_csv_format2_left_to_right(My_Labels, name, emb1_ids, emb2_ids, value_set):
    mixed = []
    mixed_one = []
    i = -1
    for (values, indices) in My_Labels:
        for id_in_batch in range(len(values)):
            i+=1
            mixed_one = []
            mixed_one.append(emb1_ids[i])
            for j in range(len(values[id_in_batch])):
                mixed_one.append(emb2_ids[indices[id_in_batch][j]])
                mixed_one.append(values[id_in_batch][j])
            mixed.append(mixed_one)
    np.savetxt(name + '_left_to_right.csv', mixed, delimiter = ',', fmt='%.10f')  #, fmt='%d'

    mixed = []
    mixed_one = []
    i = -1
    f = open(name + '_left_to_right_display', "w")
    for (values, indices) in My_Labels:
        for id_in_batch in range(len(values)):
            i+=1
            if(i%200 != 0):
                continue
            mixed_one = []
            mixed_one.append(i)
            mixed_one.append(value_set[emb1_ids[i]])
            for j in range(len(values[id_in_batch])):
                mixed_one.append(value_set[emb2_ids[indices[id_in_batch][j]]])
                mixed_one.append(values[id_in_batch][j])
            f.write(mixed_one.__str__()+"\n")
    f.close()

def save_in_csv_format2_right_to_left(My_Labels, name, emb1_ids, emb2_ids, value_set):
    mixed = []
    mixed_one = []
    i = -1
    for (values, indices) in My_Labels:
        for id_in_batch in range(len(values)):
            i+=1
            mixed_one = []
            mixed_one.append(emb2_ids[i])
            for j in range(len(values[id_in_batch])):
                mixed_one.append(emb1_ids[indices[id_in_batch][j]])
                mixed_one.append(values[id_in_batch][j])
            mixed.append(mixed_one)
    np.savetxt(name + '_right_to_left.csv', mixed, delimiter = ',', fmt='%.10f')  #, fmt='%d'

    mixed = []
    mixed_one = []
    i = -1
    f = open(name + '_right_to_left_display', "w")
    for (values, indices) in My_Labels:
        for id_in_batch in range(len(values)):
            i+=1
            if(i%200 != 0):
                continue
            mixed_one = []
            mixed_one.append(i)
            mixed_one.append(value_set[emb2_ids[i]])
            for j in range(len(values[id_in_batch])):
                mixed_one.append(value_set[emb1_ids[indices[id_in_batch][j]]])
                mixed_one.append(values[id_in_batch][j])
            f.write(mixed_one.__str__()+"\n")
    f.close()

def compute_sim(emb1, emb2, emb1_ids, emb2_ids,batch_size, value_set, context = ""):
    TOPK = 50
    start_time = time.time()
    print(context)
    with torch.no_grad():
        print("Cosine similarity of basic bert unit embedding res:")
        res_mat = []
        axis_0 = emb1.shape[0]
        emb2 = emb2.t()
        for i in tqdm(range(0,axis_0,batch_size)):
            #temp_emb1_ids = emb1_ids[i:min(i+batch_size,axis_0)]
            temp_div_mat_1 = emb1[i:min(i+batch_size,axis_0)].cuda(CUDA_NUM)
            #print(f"temp_div_mat_1.shape",temp_div_mat_1.shape)
            #print(f"emb2.shape",emb2.shape)
            res = temp_div_mat_1.mm(emb2.cuda(CUDA_NUM))  #res : [batch_size, len(emb2)]
            #for j in range(0,batch_size):
            topk_res = torch.topk(res, TOPK)
            res_mat.append((topk_res.values.cpu(), topk_res.indices.cpu()))
        #np.savetxt(ATTRIBUTEVALUE_SIM_PATH[:-4]+"_emb1_ids", emb1_ids, delimiter = ',',fmt="%s",encoding="utf-8")
        save_in_csv_format2_left_to_right(res_mat, ATTRIBUTEVALUE_SIM_PATH[:-4], emb1_ids, emb2_ids, value_set) # >>(4.12GB).
        
        res_mat = []
        emb1 = emb1.t()
        emb2 = emb2.t()
        axis_0 = emb2.shape[0]
        for i in tqdm(range(0,axis_0,batch_size)):
            temp_div_mat_1 = emb2[i:min(i+batch_size,axis_0)].cuda(CUDA_NUM)
            res = temp_div_mat_1.mm(emb1.cuda(CUDA_NUM))  #res : [batch_size, len(emb1)]
            topk_res = torch.topk(res, TOPK)
            res_mat.append((topk_res.values.cpu(), topk_res.indices.cpu()))
        save_in_csv_format2_right_to_left(res_mat, ATTRIBUTEVALUE_SIM_PATH[:-4], emb1_ids, emb2_ids, value_set) # >>(4.12GB).

    print("compute_sim using time: {:.3f}".format(time.time()-start_time))
    print("--------------------")


def main():
    print("----------------get attribute value embedding--------------------")
    cuda_num = CUDA_NUM
    print("GPU NUM :",cuda_num)
    ent_ill, index2rel, index2entity, \
    rel2index, entity2index, \
    rel_triples_1, rel_triples_2, entid_1, entid_2 = read_structure_datas(DATA_PATH)


    #load attribute triples files.
    ents = [ent for ent in entity2index.keys()]
    new_attribute_triple_1_file_path = DATA_PATH + 'new_att_triples_1'
    new_attribute_triple_2_file_path = DATA_PATH + 'new_att_triples_2'
    print("loading attribute triples from: ",new_attribute_triple_1_file_path)
    print("loading attribute triples from: ",new_attribute_triple_2_file_path)
    att_datas = read_attribute_datas(new_attribute_triple_1_file_path,
                                     new_attribute_triple_2_file_path,
                                     ents,entity2index,add_name_as_attTriples = False)

    #Remove duplicate attribute values (['a','a','a','b','b'] -> ['a','b'])
    value_set = []
    for e,a,l,l_type in att_datas:
        value_set.append(l)
    #before remove duplicate .. all attribute value num: 974020
    #after remove duplicate .. all attribute value num: 342597
    #before remove duplicate .. all attribute value num: 935060   add_name_as_attTriples = False
    #after remove duplicate .. all attribute value num: 324120
    print("before remove duplicate .. all attribute value num: {}".format(len(value_set)))
    value_set = list( set(value_set) )
    print("after remove duplicate .. all attribute value num: {}".format(len(value_set)))
    value_set.sort(key=lambda x:len(x))
    np.savetxt(ATTRIBUTEVALUE_LIST_PATH[:-4], value_set, delimiter = ',',fmt="%s",encoding="utf-8")
    value_to_id = dict()
    for i in range(len(value_set)):
        value_to_id[value_set[i]] = i

    kg1_value_set = set()
    kg2_value_set = set()
    with open(new_attribute_triple_1_file_path,"r",encoding="utf-8") as f:
        for line in f:
            e, a, l, l_type = line.rstrip().split('\t')
            kg1_value_set.add(value_to_id[l])
    with open(new_attribute_triple_2_file_path,"r",encoding="utf-8") as f:
        for line in f:
            e, a, l, l_type = line.rstrip().split('\t')
            kg2_value_set.add(value_to_id[l])
    kg1_value_set = list(kg1_value_set)
    kg1_value_set.sort(key=lambda x:x)
    #print(f"kg1_value_set:{kg1_value_set}")
    kg2_value_set = list(kg2_value_set)
    kg2_value_set.sort(key=lambda x:x)

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


    #get attributeValue_embedding by basic bert unit model.
    Tokenizer = BertTokenizer.from_pretrained('../bert-base-multilingual-cased')
    value_emb = attributeValue_emb_gene(value_set,Model,Tokenizer,
                           batch_size=2048,cuda_num = cuda_num,max_length=64)
    
    #save_in_csv_format(value_emb, ATTRIBUTEVALUE_EMB_PATH[:-4])
    #pickle.dump(value_emb,open(ATTRIBUTEVALUE_EMB_PATH,"wb"))
    
    #pickle.dump(value_set,open(ATTRIBUTEVALUE_LIST_PATH,"wb"))
    #print("save attributeValue embedding in: ",ATTRIBUTEVALUE_EMB_PATH)
    print("save attributeValue list in: ",ATTRIBUTEVALUE_LIST_PATH)
    print("attribute embedding shape:",np.array(value_emb).shape,"\nattribute list length:",len(value_set))

    emb1 = []
    emb2 = []
    emb1_ids = []
    emb2_ids = []
    for id in kg1_value_set:
        emb1_ids.append(id)
        emb1.append(torch.tensor(value_emb[id]))
    print(torch.tensor(value_emb[id]).shape)  #torch.Size([300])
    emb1 = torch.stack(emb1,0)                #torch.Size([171147, 300])
    print(emb1.shape)
    for id in kg2_value_set:
        emb2_ids.append(id)
        emb2.append(torch.tensor(value_emb[id]))
    emb2 = torch.stack(emb2,0)  #emb2  198810x300
    print(emb2.shape)
    compute_sim(emb1, emb2, emb1_ids, emb2_ids, BATCH_SIZE, value_set)
    




if __name__ == '__main__':
    fixed(SEED_NUM)
    main()