# %%
from subprocess import run

from joblib import Parallel, delayed
from pandas import DataFrame

TIMES = 100000


def decode_out(x) -> int:
    return int(x.stdout.decode('utf-8'))


java_data_list = Parallel(n_jobs=-1, backend='threading')(delayed(run)(
    ['java', 'MementoEvalTest$Main'], check=False, capture_output=True) for _ in range(TIMES))
slimejava_data_list = Parallel(n_jobs=-1, backend='threading')(delayed(run)(
    ['java', '-classpath', 'gen', 'MementoEvalTest$Main'], check=False, capture_output=True) for _ in range(TIMES))
df = DataFrame({'Java': map(decode_out, java_data_list),
               'SlimeJava': map(decode_out, slimejava_data_list)})
axes = df.boxplot()
axes.get_figure().suptitle('spentTime')
# %%
