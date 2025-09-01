# NALA
Source code [Experiments, Datasets] for "NALA: an Effective and Interpretable Entity Alignment Method"
(Findings of the Association for Computational Linguistics: EMNLP 2024)[paper link in acl anthology](https://aclanthology.org/2024.findings-emnlp.806/)

## Source code references
The source code of some parts of this method is adapted from the corresponding repositories. 
The repositories are:
- [BERT-INT](https://github.com/kosugi11037/bert-int) for BERT-INT.
- [PARIS](https://github.com/dig-team/PARIS) for PARIS.
- [PARIS+](https://github.com/epfl-dlab/entity-matchers) for entity-matchers (PARIS+).

## Installation process

Create a virtual environment with Anaconda to run NALAoverall and install the imported python packages if needed. (fine for python3.8 and numpy)

If you want to run the BERT unit, create an environment (fine for python3.8, torch1.12, CUDA11.6 and transformers) to run experiments with it.

Download the datasets, pretrained BERT model and some experiment results (with evidence log file): you can find them following the link [https://figshare.com/s/37e2d85e87e35d90d2e5](https://figshare.com/s/37e2d85e87e35d90d2e5). 
Extract the zip and place the contents of "datasets" into the empty folder "NALA/datasets". 
As the file is big, we provide a light version here [https://figshare.com/s/d094d20a960a42979df2](https://figshare.com/s/d094d20a960a42979df2) (without pretrained bert model and embeddings).

The DBP15k datasets and OpenEA benchmark datasets are consistent with other studies, with urls of entities and relations abbreviated for simplicity.
## Reproduction of results

run experiment with NALA's similarity inference module and matching module 
```shell
conda activate **environment_name**
cd NALAoverall
nohup python3 -u run_experiment.py --dataset DBP15k_zh_en --dataset_division 721_1folds --table_setting 1 --bootstrap 2 > ./output/DBP15K_zh_en_result.log &
```
(change the argument "table_setting" for different configuration groups (1-5) and ablation studies (6-11))

finetune BERT unit
```shell
**update parameters in Param.py**
cd BERTunit/basic_bert_unit
nohup python3 -u main.py > ../result.log &
```

get attribute value embedding similarity & entity embedding similarity
```shell
**update parameters in Param.py**
cd BERTunit/embedding_model
python clean_attribute_data.py
nohup python3 -u get_attributeValue_embedding.py > ./result.log &
python get_entity_embedding.py
```
due to code refactoring, the reproduced results may be slightly different with the reported result