import argparse
import os
import random
"""
def strip_bracket(str):
    if str[0] == "<":
        str = str[1:]
    if str[-1] == ">":
        str = str[:-1]
    return str   
"""
abbreviate_dict = {"http://dbpedia.org/resource/":"dbp_en:","http://dbpedia.org/property/":"dbp_en_prop:","http://zh.dbpedia.org/resource/":"dbp_zh:","http://zh.dbpedia.org/property/":"dbp_zh_prop:"
                   ,"http://fr.dbpedia.org/resource/":"dbp_fr:","http://fr.dbpedia.org/property/":"dbp_fr_prop:"
                   ,"http://ja.dbpedia.org/resource/":"dbp_ja:","http://ja.dbpedia.org/property/":"dbp_ja_prop:"
                   ,"http://www.w3.org/2001/XMLSchema#":"xsd:","http://dbpedia.org/datatype/":"dbp_type:"
                   ,"http://xmlns.com/foaf/0.1/":"foaf:"
                   ,"http://purl.org/dc/elements/1.1/":"purl:"}

def abbreviate(str1, r=0, attribute=0):
    for full in abbreviate_dict.keys():
        if str1.startswith(full):
            str1 = abbreviate_dict[full] + str1[len(full):]
            break
    if r:
        if attribute and not str1.startswith("attr_"):
            str1 = "attr_" + str1
        elif not attribute and not str1.startswith("rel_"):
            str1 = "rel_" + str1
    return str1

def un_abbreviate(str1, r=0, attribute=0):
    rdict = dict(zip(abbreviate_dict.values(), abbreviate_dict.keys()))
    for short in rdict.keys():
        if str1.startswith(short):
            str1 = rdict[short] + str1[len(short):]
            break
    if r:
        if attribute and not str1.startswith("attr_"):
            str1 = "attr_" + str1
        elif not attribute and not str1.startswith("rel_"):
            str1 = "rel_" + str1
    return str1


def abbreviate_attribute_value(str1):
    pos = str1.find("^^")
    if pos==-1:
        return abbreviate(str1, 0)
    pos += 2 
    return str1[:pos] + strip_bracket_and_abbreviate(str1[pos:], 0, 0)

def strip_bracket_and_abbreviate(str, r, attribute):
    return abbreviate(str.lstrip('<').rstrip('>'), r, attribute)

def split3(l, with_dot_and_space = 0, attribute = 0):
    if (not l.rstrip("\n")):
        return None
    if with_dot_and_space:
        list1 = l.rstrip("\n").rstrip(".").rstrip().split("\t", maxsplit = 2)
    else:
        list1 = l.rstrip("\n").split("\t", maxsplit = 2)
    if (len(list1) != 3):
        if with_dot_and_space:
            list1 = l.rstrip("\n").rstrip(".").rstrip().split(" ", maxsplit = 2)
        else:
            list1 = l.rstrip("\n").split(" ", maxsplit = 2)

        if (len(list1) != 3):
            return None

    (e1, r, a) = list1
    e1 = strip_bracket_and_abbreviate(e1, 0, 0)
    r = strip_bracket_and_abbreviate(r, 1, attribute)
    if attribute:
        a = abbreviate_attribute_value(a)
    else:
        a = strip_bracket_and_abbreviate(a, 0, 0)
    return (e1, r, a)

def main(root_folder, dataset):
    new_dataset_folder = root_folder + "/" + dataset
    #command = "cp -r {} {}".format(root_folder + "/" + dataset, new_dataset_folder)
    #os.system(command)
    ids = {}
    fs1_entities = set()
    #读取19000+有编号（缩减版）实体
    lines = []
    with open(new_dataset_folder + "/ent_ids_1", encoding = "utf8") as f:
        for l in f:
            (id, e1) = l.rstrip("\n").split("\t")
            fs1_entities.add(un_abbreviate(e1))

    fs2_entities = set()
    #读取19000+有编号（缩减版）实体
    lines = []
    with open(new_dataset_folder + "/ent_ids_2", encoding = "utf8") as f:
        for l in f:
            (id, e1) = l.rstrip("\n").split("\t")
            fs2_entities.add(un_abbreviate(e1))


    lines = []
    with open(new_dataset_folder + "/test_links", encoding = "utf8") as f:
        e1_not_in = 0
        e2_not_in = 0
        for l in f:
            #print(l.rstrip("\n").rstrip(".").rstrip().split("\t", maxsplit = 2))
            if (not l.rstrip("\n")):
                continue
            (e1,e2) = l.rstrip("\n").split("\t", maxsplit = 1)
            e1 = un_abbreviate(e1)
            e2 = un_abbreviate(e2)
            if e1 not in fs1_entities:
                print(f"ent_links e1 not in fs1_entities: {e1}")
                e1_not_in += 1
            if e2 not in fs2_entities:
                print(f"ent_links e2 not in fs2_entities: {e2}")
                e2_not_in += 1
            lines.append("{}\t{}\n".format(e1, e2))
        print(f"e1 not in: {e1_not_in}")
        print(f"e2 not in: {e2_not_in}")
    with open(new_dataset_folder + "/ref_pairs", "w", encoding = "utf8") as f:
        f.writelines(lines)

    lines = []
    with open(new_dataset_folder + "/train_links", encoding = "utf8") as f:
        e1_not_in = 0
        e2_not_in = 0
        for l in f:
            #print(l.rstrip("\n").rstrip(".").rstrip().split("\t", maxsplit = 2))
            if (not l.rstrip("\n")):
                continue
            (e1,e2) = l.rstrip("\n").split("\t", maxsplit = 1)
            e1 = un_abbreviate(e1)
            e2 = un_abbreviate(e2)
            if e1 not in fs1_entities:
                print(f"ent_links e1 not in fs1_entities: {e1}")
                e1_not_in += 1
            if e2 not in fs2_entities:
                print(f"ent_links e2 not in fs2_entities: {e2}")
                e2_not_in += 1
            lines.append("{}\t{}\n".format(e1, e2))
        print(f"e1 not in: {e1_not_in}")
        print(f"e2 not in: {e2_not_in}")
    with open(new_dataset_folder + "/valid_links", encoding = "utf8") as f:
        e1_not_in = 0
        e2_not_in = 0
        for l in f:
            #print(l.rstrip("\n").rstrip(".").rstrip().split("\t", maxsplit = 2))
            if (not l.rstrip("\n")):
                continue
            (e1,e2) = l.rstrip("\n").split("\t", maxsplit = 1)
            e1 = un_abbreviate(e1)
            e2 = un_abbreviate(e2)
            if e1 not in fs1_entities:
                print(f"ent_links e1 not in fs1_entities: {e1}")
                e1_not_in += 1
            if e2 not in fs2_entities:
                print(f"ent_links e2 not in fs2_entities: {e2}")
                e2_not_in += 1
            lines.append("{}\t{}\n".format(e1, e2))
        print(f"e1 not in: {e1_not_in}")
        print(f"e2 not in: {e2_not_in}")
    with open(new_dataset_folder + "/sup_pairs", "w", encoding = "utf8") as f:
        f.writelines(lines)


if __name__ == "__main__":

    #main("/home/2022xuch/paris/datasets", "DBP15k_full_zh_en_2/供bertint")
    #main("/home/2022xuch/paris/datasets", "DBP15k_full_ja_en_2/供bertint")
    main("/home/2022xuch/paris/datasets", "DBP15k_full_fr_en_2/供bertint")
