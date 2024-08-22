# NALA
Source code for "NALA: an Effective and Interpretable Entity Alignment Method [Experiments, Datasets]"

## Source code references
The source code for some parts of our method has been adapted from the corresponding repositories. 
The repositories are:
- [BERT-INT](https://github.com/kosugi11037/bert-int) for BERT-INT.
- [PARIS](https://github.com/dig-team/PARIS) for PARIS.
- [PARIS+](https://github.com/epfl-dlab/entity-matchers) for entity-matchers(PARIS+).

## Installation process

Create a virtual environment with Anaconda to run NALAoverall and install the imported python packages if needed. (fine for python3.8 and numpy)

If you want to run the BERT unit, create an environment (fine for python3.8, torch1.12, CUDA11.6 and transformers) to run experiments with it.

Download the datasets, pretrained BERT model and some experiment results (with evidence log file): you can find them following the link [https://figshare.com/s/dc9d168a1afe1db073be](https://figshare.com/s/dc9d168a1afe1db073be). 
Extract the zip and place the contents of "datasets" into the empty folder "NALA/datasets". 
Place the contents of "data(for BERT)" into the empty folder "NALA/BERTunit/data". 

The DBP15k and D_W_15K_V2 datasets are consistent with other studies, with entities and relations' urls abbreviated for simplicity.
## Reproduction of results

```shell
cd NALAoverall

python run_experiment.py --dataset DBP15k_full_zh_en_2 --dataset_division 721_1folds --table_setting 0
```
(change the argument "table_setting" for different configuration groups (1-5) and ablation studies (6-11))

