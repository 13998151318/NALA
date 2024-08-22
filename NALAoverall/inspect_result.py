import os
from utils import * 
import functools
import random
output_path = "/home/2022xuch/PNAL/PNALoverall/output"
#lang_path = "/zh_en"
fold_path = "/DBP15k_full_ja_en_2_0609_183522"
full_fold_path = output_path + fold_path

zero_seed = True
filter = 0#0.1

dataset_path = "/home/2022xuch/PNAL/datasets/DBP15k_full_ja_en_2"
run = 19 #21
eqv_path = full_fold_path + "/output/" + str(run) + "_eqv.tsv"
train_path = full_fold_path + "/" + "train" + "_links"
valid_path = full_fold_path + "/" + "valid" + "_links"
test_path = full_fold_path + "/" + "test" + "_links"
inspect_path = full_fold_path + "/inspect"

attr_path = dataset_path + "/attr_triples"
attr_paths = [attr_path + "_1", attr_path + "_2"]
rel_path = dataset_path + "/rel_triples"
rel_paths = [rel_path + "_1", rel_path + "_2"]

eqv = []
eqv_2 = []
with open(eqv_path, 'r', encoding='utf-8') as file:
    for line in file:
        line = line.strip('\n').split('\t')
        if len(line)!= 3:
            continue
        f = (float)(line[2].strip('%').split(';')[0])
        c = (float)(line[2].strip('%').split(';')[1])
        exp = f * c
        if exp > filter:
            eqv.append((line[0], line[1], exp, (line[2])))
            eqv_2.append((line[0], line[1]))
#print(eqv)

set_train = set()
with open(train_path, 'r', encoding='utf-8') as file:
    for line in file:
        line = line.strip('\n').split('\t')
        if len(line)!= 2:
            continue
        set_train.add((line[0], line[1]))

set_valid = set()
with open(valid_path, 'r', encoding='utf-8') as file:
    for line in file:
        line = line.strip('\n').split('\t')
        if len(line)!= 2:
            continue
        set_valid.add((line[0], line[1]))

set_test = set()
with open(test_path, 'r', encoding='utf-8') as file:
    for line in file:
        line = line.strip('\n').split('\t')
        if len(line)!= 2:
            continue
        set_test.add((line[0], line[1]))
if zero_seed:
    valid_test = set_valid | set_test | set_train
    set_train = set()
else:
    valid_test = set_valid | set_test

res_no_train = []
for align in eqv:
    if (align[0], align[1]) not in set_train:
        # Evaluate only the alignments that were not present in the training data (seed).
        res_no_train.append(align)

res_no_train_no_vt = []
for align in res_no_train:
    if (align[0], align[1]) not in valid_test:
        res_no_train_no_vt.append(align)

valid_test_no_res = []
for align in valid_test:
    if (align[0], align[1]) not in eqv_2:
        valid_test_no_res.append(align)

if not os.path.exists(inspect_path):
    os.mkdir(inspect_path)

res_in_train_or_vt = []
for align in eqv:
    if (align[0], align[1]) in set_train or (align[0], align[1]) in set_valid or (align[0], align[1]) in set_test:
        # Evaluate only the alignments that were not present in the training data (seed).
        res_in_train_or_vt.append(align)


def compute_prec_rec_f1_1(aligns, truth_links):

    aligns = set((align[0], align[1]) for align in aligns)
    truth_links = set(truth_links)
    num_correct = len(aligns.intersection(truth_links))
    if num_correct == 0 or len(aligns) == 0:
        print("Got 0, 0, 0 in evaluation!!!")
        return 0, 0, 0
    precision = num_correct / len(aligns)
    recall = num_correct / len(truth_links)
    f1 = 2 * precision * recall / (precision + recall)
    return precision, recall, f1

inspect_path1 = inspect_path + "/inspect_" + str(run) + "_eqv.tsv"
with open(inspect_path1, 'w', encoding='utf-8') as file:
    precision, recall, f1 = compute_prec_rec_f1_1(res_no_train, valid_test)
    file.write(f"run:{run}  precision:{precision:.5f}   recall:{recall:.5f} f1:{f1:.5f} len(res_no_train):{len(res_no_train)} len(res_no_train_no_vt):{len(res_no_train_no_vt)}\n")
    file.write(f"len(valid_test_no_res):{len(valid_test_no_res)}\n valid_test_no_res: \n")

    res_no_train_no_vt.sort(key = lambda x:x[2]) #functools.cmp_to_key(compare_1)
    res_in_train_or_vt.sort(key = lambda x:x[2])
    valid_test_no_res.sort(key = lambda x:x[0])

    for align in valid_test_no_res:
        file.write(f"{align[0]:<40}{align[1]:<40} \n")
    
    file.write(f"\n\n\n\n\n\n\n\n\n\n\n\n len(res_no_train_no_vt):{len(res_no_train_no_vt)}  \n\n")
    for align in res_no_train_no_vt:
        file.write(f"{align[0]:<40}{align[1]:<60}{align[2]:<40} \n")
    
    file.write(f"\n\n\n\n\n\n\n\n\n\n\n\n len(res_in_train_or_vt):{len(res_in_train_or_vt)}  \n\n")
    for align in res_in_train_or_vt:
        file.write(f"{align[0]:<40}{align[1]:<60}{align[2]:<40} \n")
    
    


def inspect_entity(graph, entity_name):
    file_str = ""
    file_str += "----------------------------------------------------------------------------------------------------------\n"
    if graph == 0:
        file_str += f"检视  \"{entity_name}\"   ，该实体属于左图谱\n"
    elif graph == 1:
        file_str += f"检视  \"{entity_name}\" ，该实体属于右图谱\n"
    file_str += f"检视属性三元组：\n"
    with open(attr_paths[graph], 'r', encoding='utf-8') as file:
        attr_list = []
        for line in file:
            line_split = line.strip('\n').split('\t')
            if len(line_split)!= 3:
                continue
            if line_split[0] == entity_name:
                attr_list.append(line.strip('\n'))
        attr_list.sort()
        for attr in attr_list:
            file_str += f"{attr}\n"
    
    file_str += f"检视关系三元组：\n"
    with open(rel_paths[graph], 'r', encoding='utf-8') as file:
        rel_list1 = []
        for line in file:
            line_split = line.strip('\n').split('\t')
            if len(line_split)!= 3:
                continue
            if line_split[0] == entity_name:
                rel_list1.append(line.strip('\n'))
        rel_list1.sort()
        for rel in rel_list1:
            file_str += f"{rel}\n"
    with open(rel_paths[graph], 'r', encoding='utf-8') as file:
        rel_list2 = []
        for line in file:
            line_split = line.strip('\n').split('\t')
            if len(line_split)!= 3:
                continue
            if line_split[2] == entity_name:
                rel_list2.append((line_split[0],line_split[1],line_split[2]))
        rel_list2.sort(key = lambda x:x[1])
        for rel in rel_list2:
            file_str += f"{rel[0]}  {rel[1]}    {rel[2]}\n"
    return file_str


def inspect_align(ratio = 1, group = valid_test_no_res, group_name = "valid_test_no_res"):
    #group = random.sample(group, len(group)//ratio)
    group = group[:len(group)//ratio]
    inspect_path2 = inspect_path + "/inspect_" + str(run) + "_eqv.tsv_" + group_name
    with open(inspect_path2, 'w', encoding='utf-8') as file:
        file.write(f"group_name: {group_name}\ngroup_len: {len(group)}\n")
        if group_name == "valid_test_no_res":
            group.sort(key = lambda x:f"{x[0]} {x[1]}")
            file.write(f"valid_test_no_res 代表验证和测试集的这些正确对齐 未在程序输出的结果中出现，拉低了recall值和hit@1值\n这一组中抽取出来的全部实体对(抽取比例1:{ratio}):\n")
            for k in range(len(group)):
                align = group[k]
                file.write(f"第{k}对:   {align[0]} {align[1]}\n")

                file.write(f"左实体 \"{align[0]}\"：\n")
                appear = 0
                for align1 in res_no_train:
                    if align[0] == align1[0]:
                        file.write(f"出现在这条错误结果之中： {align1[0]}  {align1[1]} {align1[2]}\n")
                        appear = 1
                if appear == 0:
                    file.write(f"未出现\n")

                file.write(f"右实体 \"{align[1]}\" ：\n")
                appear = 0
                for align1 in res_no_train:
                    if align[1] == align1[1]:
                        file.write(f"出现在这条错误结果之中： {align1[0]}  {align1[1]} {align1[2]}\n")
                        appear = 1
                if appear == 0:
                    file.write(f"未出现\n")
            
            for k in range(len(group)): 
                align = group[k]
                for ii in range(3):
                    file.write(f"----------------------------------------------------------------------------------------------------------\n")
                file.write(f"检视group中第{k}个对齐\n{align[0]} {align[1]}\n")
                file.write(f"人工分析未能对齐原因：\n")
                file.write(f"{inspect_entity(0, align[0])}\n")
                file.write(f"{inspect_entity(1, align[1])}\n")
                file.write(f"左实体 \"{align[0]}\"   是否在程序输出结果（res）中出现，以及出现的位置：\n")
                appear = 0
                for align1 in res_no_train:
                    if align[0] == align1[0]:
                        file.write(f"出现了，有在这条（错误）结果之中： {align1[0]}  {align1[1]} {align1[2]}\n")
                        file.write(f"人工分析对齐错误原因：\n")
                        appear = 1
                        file.write(f"{inspect_entity(1, align1[1])}\n")
                if appear == 0:
                    file.write(f"未出现\n")

                file.write(f"右实体 \"{align[1]}\"   是否在程序输出结果（res）中出现，以及出现的位置：\n")
                appear = 0
                for align1 in res_no_train:
                    if align[1] == align1[1]:
                        file.write(f"出现了，有在这条（错误）结果之中： {align1[0]}  {align1[1]} {align1[2]}\n")
                        file.write(f"人工分析对齐错误原因：\n")
                        appear = 1
                        file.write(f"{inspect_entity(0, align1[0])}\n")
                if appear == 0:
                    file.write(f"未出现\n")


        elif group_name == "res_no_train_no_vt": 
            file.write(f"res_no_train_no_vt 代表程序输出的结果中 未在验证和测试集的这些正确对齐中出现的那些结果，拉低了precision值\n")
            group.sort(key = lambda x:x[2])
            for align in group:
                file.write(f"{align[0]} {align[1]} {align[2]}\n")
            for k in range(len(group)): 
                align = group[k]
                for ii in range(3):
                    file.write(f"----------------------------------------------------------------------------------------------------------\n")
                file.write(f"检视group中第{k}个对齐{align[0]} {align[1]} {align[2]}\n")
                file.write(f"人工分析对齐错误（或者正确但超出范围）原因：\n")
                file.write(f"{inspect_entity(0, align[0])}\n")
                file.write(f"{inspect_entity(1, align[1])}\n")
                file.write(f"左实体 \"{align[0]}\"   是否在验证或测试集中出现，以及出现的位置：\n")
                appear = 0
                for align1 in valid_test:
                    if align[0] == align1[0]:
                        file.write(f"出现了，有在这条之中：{align1[0]}  {align1[1]}\n")
                        appear = 1
                        file.write(f"{inspect_entity(1, align1[1])}\n")
                if appear == 0:
                    file.write(f"未出现\n")
                    
                file.write(f"右实体 \"{align[1]}\"   是否在验证或测试集中出现，以及出现的位置：\n")
                appear = 0
                for align1 in valid_test:
                    if align[1] == align1[1]:
                        file.write(f"出现了，有在这条之中： {align1[0]}  {align1[1]}\n")
                        appear = 1
                        file.write(f"{inspect_entity(0, align1[0])}\n")
                if appear == 0:
                    file.write(f"未出现\n")

#inspect_align()
#inspect_align(1)
#inspect_align(1, res_no_train_no_vt, "res_no_train_no_vt")
#print(inspect_entity(1, "dbp_en:Gorillaz"))

#右实体 "dbp_en:2003–04_UEFA_Cup"   是否在程序输出结果（res）中出现，以及出现的位置：

#print(inspect_entity(1, "dbp_en:Mark_Begich"))