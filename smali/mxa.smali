.class public Lmxa;
.super Lpdy;
.source "PG"


# static fields
.field private static final a:Lwpm;

.field protected static final b:J

.field public static final synthetic e:I


# instance fields
.field public c:Z

.field public d:Lqfi;

.field private g:Lqzc;

.field private h:Z

.field private i:Lmxc;

.field private j:Lpqt;

.field private k:Lrym;


# direct methods
.method static constructor <clinit>()V
    .locals 2

    .line 1
    invoke-static {}, Landroid/os/SystemClock;->elapsedRealtime()J

    .line 2
    .line 3
    .line 4
    move-result-wide v0

    .line 5
    sput-wide v0, Lmxa;->b:J

    .line 6
    .line 7
    const-string v0, "com/google/android/libraries/inputmethod/application/impl/AppBase"

    .line 8
    .line 9
    invoke-static {v0}, Lwpm;->i(Ljava/lang/String;)Lwpm;

    .line 10
    .line 11
    .line 12
    move-result-object v0

    .line 13
    sput-object v0, Lmxa;->a:Lwpm;

    .line 14
    .line 15
    return-void
.end method

.method public constructor <init>()V
    .locals 0

    .line 1
    invoke-direct {p0}, Lpdy;-><init>()V

    .line 2
    .line 3
    .line 4
    return-void
.end method


# virtual methods
.method protected final attachBaseContext(Landroid/content/Context;)V
    .locals 0

    .line 1
    sput-object p1, Lpud;->appCtx:Landroid/content/Context;

    .line 2
    .line 3
    invoke-super {p0, p1}, Lpdy;->attachBaseContext(Landroid/content/Context;)V

    .line 4
    .line 5
    .line 6
    const/4 p1, 0x0

    .line 7
    invoke-static {p0, p1}, Lvqt;->b(Landroid/content/Context;Z)Z

    .line 8
    .line 9
    .line 10
    return-void
.end method

.method public d(Lqpn;)V
    .locals 0

    .line 1
    const-string p0, "AppBase.applyPreferenceValues"

    .line 2
    .line 3
    invoke-static {p0}, Landroid/os/Trace;->beginSection(Ljava/lang/String;)V

    .line 4
    .line 5
    .line 6
    invoke-static {}, Landroid/os/Trace;->endSection()V

    .line 7
    .line 8
    .line 9
    return-void
.end method

.method protected e()V
    .locals 0

    .line 1
    return-void
.end method

.method protected f()V
    .locals 0

    .line 1
    const-string p0, "AppBase.onUserUnlocked"

    .line 2
    .line 3
    invoke-static {p0}, Landroid/os/Trace;->beginSection(Ljava/lang/String;)V

    .line 4
    .line 5
    .line 6
    invoke-static {}, Landroid/os/Trace;->endSection()V

    .line 7
    .line 8
    .line 9
    return-void
.end method

.method public final onConfigurationChanged(Landroid/content/res/Configuration;)V
    .locals 5

    .line 1
    sget-object v0, Lmxa;->a:Lwpm;

    .line 2
    .line 3
    invoke-virtual {v0}, Lwpc;->b()Lwqa;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    check-cast v0, Lwpj;

    .line 8
    .line 9
    const/16 v1, 0x11b

    .line 10
    .line 11
    const-string v2, "AppBase.java"

    .line 12
    .line 13
    const-string v3, "com/google/android/libraries/inputmethod/application/impl/AppBase"

    .line 14
    .line 15
    const-string v4, "onConfigurationChanged"

    .line 16
    .line 17
    invoke-interface {v0, v3, v4, v1, v2}, Lwpj;->j(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)Lwqa;

    .line 18
    .line 19
    .line 20
    move-result-object v0

    .line 21
    check-cast v0, Lwpj;

    .line 22
    .line 23
    const-string v1, "onConfigurationChanged() : NewConfig = %s"

    .line 24
    .line 25
    invoke-interface {v0, v1, p1}, Lwpj;->w(Ljava/lang/String;Ljava/lang/Object;)V

    .line 26
    .line 27
    .line 28
    invoke-super {p0, p1}, Lpdy;->onConfigurationChanged(Landroid/content/res/Configuration;)V

    .line 29
    .line 30
    .line 31
    invoke-virtual {p0}, Lmxa;->getApplicationContext()Landroid/content/Context;

    .line 32
    .line 33
    .line 34
    move-result-object p0

    .line 35
    invoke-static {p0}, Lqyv;->d(Landroid/content/Context;)V

    .line 36
    .line 37
    .line 38
    return-void
.end method

.method public onCreate()V
    .locals 11

    .line 1
    const-string v0, "AppBase.onCreate"

    .line 2
    .line 3
    invoke-static {v0}, Landroid/os/Trace;->beginSection(Ljava/lang/String;)V

    .line 4
    .line 5
    .line 6
    const-string v0, "AppBase.java"

    .line 7
    .line 8
    :try_start_0
    invoke-virtual {p0}, Lmxa;->getApplicationContext()Landroid/content/Context;

    .line 9
    .line 10
    .line 11
    move-result-object v1

    .line 12
    invoke-static {v1}, Lrzr;->b(Landroid/content/Context;)Z

    .line 13
    .line 14
    .line 15
    move-result v2

    .line 16
    iput-boolean v2, p0, Lmxa;->c:Z

    .line 17
    .line 18
    iget-boolean v2, p0, Lmxa;->h:Z

    .line 19
    .line 20
    if-nez v2, :cond_4

    .line 21
    .line 22
    const/4 v2, 0x1

    .line 23
    iput-boolean v2, p0, Lmxa;->h:Z

    .line 24
    .line 25
    invoke-static {}, Landroid/os/SystemClock;->elapsedRealtime()J

    .line 26
    .line 27
    .line 28
    move-result-wide v3

    .line 29
    invoke-super {p0}, Lpdy;->onCreate()V

    .line 30
    .line 31
    .line 32
    const-string v5, "train"

    .line 33
    .line 34
    invoke-static {v1, v5}, Lrzr;->c(Landroid/content/Context;Ljava/lang/String;)Z

    .line 35
    .line 36
    .line 37
    move-result v5

    .line 38
    iget-boolean v6, p0, Lmxa;->c:Z

    .line 39
    .line 40
    if-nez v6, :cond_0

    .line 41
    .line 42
    if-nez v5, :cond_0

    .line 43
    .line 44
    sget-object p0, Lmxa;->a:Lwpm;

    .line 45
    .line 46
    invoke-virtual {p0}, Lwpc;->b()Lwqa;

    .line 47
    .line 48
    .line 49
    move-result-object p0

    .line 50
    check-cast p0, Lwpj;

    .line 51
    .line 52
    const-string v1, "com/google/android/libraries/inputmethod/application/impl/AppBase"

    .line 53
    .line 54
    const-string v2, "onCreate"

    .line 55
    .line 56
    const/16 v3, 0x7c

    .line 57
    .line 58
    invoke-interface {p0, v1, v2, v3, v0}, Lwpj;->j(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)Lwqa;

    .line 59
    .line 60
    .line 61
    move-result-object p0

    .line 62
    check-cast p0, Lwpj;

    .line 63
    .line 64
    const-string v0, "Not running in main or train process, skipping further initialization."

    .line 65
    .line 66
    invoke-interface {p0, v0}, Lwpj;->t(Ljava/lang/String;)V

    .line 67
    .line 68
    .line 69
    goto/16 :goto_1

    .line 70
    .line 71
    :cond_0
    invoke-static {}, Lnhg;->a()Lnhg;

    .line 72
    .line 73
    .line 74
    move-result-object v0

    .line 75
    iget-object v0, v0, Lnhg;->c:Lxkf;

    .line 76
    .line 77
    new-instance v5, Llnt;

    .line 78
    .line 79
    const/16 v6, 0x9

    .line 80
    .line 81
    invoke-direct {v5, v1, v6}, Llnt;-><init>(Ljava/lang/Object;I)V

    .line 82
    .line 83
    .line 84
    invoke-interface {v0, v5}, Lxkf;->execute(Ljava/lang/Runnable;)V

    .line 85
    .line 86
    .line 87
    invoke-static {v1}, Lqpn;->I(Landroid/content/Context;)Lqpn;

    .line 88
    .line 89
    .line 90
    move-result-object v0

    .line 91
    invoke-static {}, Lofh;->a()V

    .line 92
    .line 93
    .line 94
    new-instance v5, Lpzi;

    .line 95
    .line 96
    invoke-direct {v5}, Lpzi;-><init>()V

    .line 97
    .line 98
    .line 99
    sget v6, Lpxx;->a:I

    .line 100
    .line 101
    sget-object v6, Lpya;->e:Ljava/util/concurrent/atomic/AtomicReference;

    .line 102
    .line 103
    invoke-virtual {v6, v5}, Ljava/util/concurrent/atomic/AtomicReference;->set(Ljava/lang/Object;)V

    .line 104
    .line 105
    .line 106
    invoke-virtual {v5}, Lpzi;->v()V

    .line 107
    .line 108
    .line 109
    invoke-virtual {v5, v0}, Lpzi;->m(Lqpn;)V

    .line 110
    .line 111
    .line 112
    invoke-static {}, Lryv;->r()Z

    .line 113
    .line 114
    .line 115
    move-result v7

    .line 116
    if-nez v7, :cond_1

    .line 117
    .line 118
    sget-boolean v7, Lryv;->b:Z

    .line 119
    .line 120
    if-eqz v7, :cond_2

    .line 121
    .line 122
    :cond_1
    invoke-virtual {v5}, Lpzi;->y()V

    .line 123
    .line 124
    .line 125
    :cond_2
    new-instance v5, Lrym;

    .line 126
    .line 127
    invoke-direct {v5}, Lrym;-><init>()V

    .line 128
    .line 129
    .line 130
    iput-object v5, p0, Lmxa;->k:Lrym;

    .line 131
    .line 132
    invoke-virtual {v5, v1}, Lrym;->c(Landroid/content/Context;)V

    .line 133
    .line 134
    .line 135
    new-instance v5, Lmxc;

    .line 136
    .line 137
    invoke-direct {v5, v1}, Lmxc;-><init>(Landroid/content/Context;)V

    .line 138
    .line 139
    .line 140
    iput-object v5, p0, Lmxa;->i:Lmxc;

    .line 141
    .line 142
    invoke-virtual {v5}, Lmxc;->a()V

    .line 143
    .line 144
    .line 145
    const-class v5, Lpqr;

    .line 146
    .line 147
    invoke-static {v1, v5}, Lzej;->f(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;

    .line 148
    .line 149
    .line 150
    move-result-object v5

    .line 151
    check-cast v5, Lpqr;

    .line 152
    .line 153
    invoke-interface {v5}, Lpqr;->aj()Lpqt;

    .line 154
    .line 155
    .line 156
    move-result-object v5

    .line 157
    iput-object v5, p0, Lmxa;->j:Lpqt;

    .line 158
    .line 159
    invoke-interface {v5}, Lpqt;->a()V

    .line 160
    .line 161
    .line 162
    new-instance v5, Lqzc;

    .line 163
    .line 164
    invoke-direct {v5, v1}, Lqzc;-><init>(Landroid/content/Context;)V

    .line 165
    .line 166
    .line 167
    iput-object v5, p0, Lmxa;->g:Lqzc;

    .line 168
    .line 169
    invoke-virtual {v5}, Lqzc;->c()V

    .line 170
    .line 171
    .line 172
    invoke-virtual {p0, v0}, Lmxa;->d(Lqpn;)V

    .line 173
    .line 174
    .line 175
    invoke-static {v1}, Lqgk;->a(Landroid/content/Context;)Lqgk;

    .line 176
    .line 177
    .line 178
    move-result-object v0

    .line 179
    invoke-virtual {v0}, Lqgk;->c()V

    .line 180
    .line 181
    .line 182
    invoke-static {v1}, Lmwt;->a(Landroid/content/Context;)V

    .line 183
    .line 184
    .line 185
    invoke-static {v1}, Lqyv;->d(Landroid/content/Context;)V

    .line 186
    .line 187
    .line 188
    invoke-virtual {p0}, Lmxa;->e()V

    .line 189
    .line 190
    .line 191
    invoke-virtual {v6}, Ljava/util/concurrent/atomic/AtomicReference;->get()Ljava/lang/Object;

    .line 192
    .line 193
    .line 194
    move-result-object v0

    .line 195
    check-cast v0, Lpya;

    .line 196
    .line 197
    sget-object v1, Lmwu;->a:Lmwu;

    .line 198
    .line 199
    sget-wide v7, Lmxa;->b:J

    .line 200
    .line 201
    invoke-static {v7, v8}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    .line 202
    .line 203
    .line 204
    move-result-object v5

    .line 205
    new-array v7, v2, [Ljava/lang/Object;

    .line 206
    .line 207
    const/4 v8, 0x0

    .line 208
    aput-object v5, v7, v8

    .line 209
    .line 210
    invoke-interface {v0, v1, v7}, Lpya;->n(Lpyf;[Ljava/lang/Object;)V

    .line 211
    .line 212
    .line 213
    sget-object v0, Lmwx;->a:Lqfh;

    .line 214
    .line 215
    invoke-static {v0}, Lqfl;->f(Lqfh;)Z

    .line 216
    .line 217
    .line 218
    invoke-static {}, Lryq;->a()Z

    .line 219
    .line 220
    .line 221
    move-result v0

    .line 222
    new-instance v1, Lmwz;

    .line 223
    .line 224
    invoke-direct {v1, p0, v0, v8}, Lmwz;-><init>(Landroid/content/ContextWrapper;ZI)V

    .line 225
    .line 226
    .line 227
    sget-object v5, Lryq;->a:Lqfh;

    .line 228
    .line 229
    new-instance v7, Lqfk;

    .line 230
    .line 231
    invoke-virtual {v5}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 232
    .line 233
    .line 234
    move-result-object v5

    .line 235
    const/4 v9, 0x0

    .line 236
    invoke-direct {v7, v2, v1, v9, v5}, Lqfk;-><init>(ZLjava/lang/Runnable;Ljava/lang/Runnable;Ljava/lang/Class;)V

    .line 237
    .line 238
    .line 239
    iput-object v7, p0, Lmxa;->d:Lqfi;

    .line 240
    .line 241
    sget-object p0, Lnho;->a:Lnho;

    .line 242
    .line 243
    invoke-virtual {v7, p0}, Lqfi;->e(Ljava/util/concurrent/Executor;)V

    .line 244
    .line 245
    .line 246
    invoke-static {}, Landroid/os/SystemClock;->elapsedRealtime()J

    .line 247
    .line 248
    .line 249
    move-result-wide v9

    .line 250
    sub-long/2addr v9, v3

    .line 251
    invoke-virtual {v6}, Ljava/util/concurrent/atomic/AtomicReference;->get()Ljava/lang/Object;

    .line 252
    .line 253
    .line 254
    move-result-object p0

    .line 255
    check-cast p0, Lpya;

    .line 256
    .line 257
    if-eqz v0, :cond_3

    .line 258
    .line 259
    sget-object v0, Lmwy;->a:Lmwy;

    .line 260
    .line 261
    goto :goto_0

    .line 262
    :cond_3
    sget-object v0, Lmwy;->b:Lmwy;

    .line 263
    .line 264
    :goto_0
    invoke-interface {p0, v0, v9, v10}, Lpya;->p(Lpyi;J)V

    .line 265
    .line 266
    .line 267
    invoke-virtual {v6}, Ljava/util/concurrent/atomic/AtomicReference;->get()Ljava/lang/Object;

    .line 268
    .line 269
    .line 270
    move-result-object p0

    .line 271
    check-cast p0, Lpya;

    .line 272
    .line 273
    sget-object v0, Lmwu;->b:Lmwu;

    .line 274
    .line 275
    invoke-static {v3, v4}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    .line 276
    .line 277
    .line 278
    move-result-object v1

    .line 279
    invoke-static {v9, v10}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    .line 280
    .line 281
    .line 282
    move-result-object v3

    .line 283
    const/4 v4, 0x2

    .line 284
    new-array v4, v4, [Ljava/lang/Object;

    .line 285
    .line 286
    aput-object v1, v4, v8

    .line 287
    .line 288
    aput-object v3, v4, v2

    .line 289
    .line 290
    invoke-interface {p0, v0, v4}, Lpya;->n(Lpyf;[Ljava/lang/Object;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 291
    .line 292
    .line 293
    :cond_4
    :goto_1
    invoke-static {}, Landroid/os/Trace;->endSection()V

    .line 294
    .line 295
    .line 296
    return-void

    .line 297
    :catchall_0
    move-exception p0

    .line 298
    invoke-static {}, Landroid/os/Trace;->endSection()V

    .line 299
    .line 300
    .line 301
    throw p0
.end method

.method public final onTrimMemory(I)V
    .locals 5

    .line 1
    invoke-super {p0, p1}, Lpdy;->onTrimMemory(I)V

    .line 2
    .line 3
    .line 4
    const/16 p0, 0x14

    .line 5
    .line 6
    if-eq p1, p0, :cond_0

    .line 7
    .line 8
    sget v0, Lpxx;->a:I

    .line 9
    .line 10
    sget-object v0, Lpya;->e:Ljava/util/concurrent/atomic/AtomicReference;

    .line 11
    .line 12
    invoke-virtual {v0}, Ljava/util/concurrent/atomic/AtomicReference;->get()Ljava/lang/Object;

    .line 13
    .line 14
    .line 15
    move-result-object v0

    .line 16
    check-cast v0, Lpya;

    .line 17
    .line 18
    sget-object v1, Lpyv;->A:Lpyv;

    .line 19
    .line 20
    invoke-static {p1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 21
    .line 22
    .line 23
    move-result-object v2

    .line 24
    const/4 v3, 0x1

    .line 25
    new-array v3, v3, [Ljava/lang/Object;

    .line 26
    .line 27
    const/4 v4, 0x0

    .line 28
    aput-object v2, v3, v4

    .line 29
    .line 30
    invoke-interface {v0, v1, v3}, Lpya;->n(Lpyf;[Ljava/lang/Object;)V

    .line 31
    .line 32
    .line 33
    :cond_0
    if-eq p1, p0, :cond_1

    .line 34
    .line 35
    sget-object p0, Lmxa;->a:Lwpm;

    .line 36
    .line 37
    invoke-virtual {p0}, Lwpc;->b()Lwqa;

    .line 38
    .line 39
    .line 40
    move-result-object p0

    .line 41
    check-cast p0, Lwpj;

    .line 42
    .line 43
    const/16 v0, 0x114

    .line 44
    .line 45
    const-string v1, "AppBase.java"

    .line 46
    .line 47
    const-string v2, "com/google/android/libraries/inputmethod/application/impl/AppBase"

    .line 48
    .line 49
    const-string v3, "onTrimMemory"

    .line 50
    .line 51
    invoke-interface {p0, v2, v3, v0, v1}, Lwpj;->j(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)Lwqa;

    .line 52
    .line 53
    .line 54
    move-result-object p0

    .line 55
    check-cast p0, Lwpj;

    .line 56
    .line 57
    const-string v0, "onTrimMemory(): %d"

    .line 58
    .line 59
    invoke-interface {p0, v0, p1}, Lwpj;->u(Ljava/lang/String;I)V

    .line 60
    .line 61
    .line 62
    invoke-static {p1}, Lptk;->a(I)V

    .line 63
    .line 64
    .line 65
    :cond_1
    return-void
.end method
