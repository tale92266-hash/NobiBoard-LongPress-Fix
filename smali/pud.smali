.class public final Lpud;
.super Ljava/lang/Object;
.source "PG"

# interfaces
.implements Lpur;
.implements Ltja;
.implements Lpuo;


# static fields
.field public static appCtx:Landroid/content/Context;

.field private static diagCtxSrc:Ljava/lang/String;

.field private static diagFirst:Z

.field private static diagPrefs:Ljava/lang/String;

.field public static final q:Lpux;


# instance fields
.field public a:Lpuc;

.field public b:Lwhe;

.field public c:Lwhe;

.field public d:[I

.field public e:Z

.field public f:Z

.field public g:I

.field public h:Z

.field public i:Z

.field public j:Z

.field public k:I

.field public l:I

.field public m:Ljava/lang/String;

.field public n:Lway;

.field public o:Z

.field public p:Luio;


# direct methods
.method static constructor <clinit>()V
    .locals 2

    .line 1
    new-instance v0, Lpux;

    .line 2
    .line 3
    const/4 v1, 0x0

    .line 4
    invoke-direct {v0, v1}, Lpux;-><init>([B)V

    .line 5
    .line 6
    .line 7
    sput-object v0, Lpud;->q:Lpux;

    .line 8
    .line 9
    return-void
.end method

.method public constructor <init>()V
    .locals 2

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 2
    .line 3
    .line 4
    sget-object v0, Lwmm;->a:Lwhe;

    .line 5
    .line 6
    const-string v1, "of(...)"

    .line 7
    .line 8
    invoke-static {v0, v1}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 9
    .line 10
    .line 11
    iput-object v0, p0, Lpud;->b:Lwhe;

    .line 12
    .line 13
    invoke-static {v0, v1}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 14
    .line 15
    .line 16
    iput-object v0, p0, Lpud;->c:Lwhe;

    .line 17
    .line 18
    sget-object v0, Lmzt;->b:[I

    .line 19
    .line 20
    iput-object v0, p0, Lpud;->d:[I

    .line 21
    .line 22
    const/4 v0, 0x1

    .line 23
    iput-boolean v0, p0, Lpud;->i:Z

    .line 24
    .line 25
    iput-boolean v0, p0, Lpud;->j:Z

    .line 26
    .line 27
    const/4 v0, -0x1

    .line 28
    iput v0, p0, Lpud;->l:I

    .line 29
    .line 30
    return-void
.end method


# virtual methods
.method public final a(Ltjb;)V
    .locals 1

    .line 1
    invoke-static {p1, p0}, Lpup;->a(Ltjb;Lpuo;)Z

    .line 2
    .line 3
    .line 4
    move-result p0

    .line 5
    if-eqz p0, :cond_0

    .line 6
    .line 7
    return-void

    .line 8
    :cond_0
    invoke-virtual {p1}, Ltjb;->c()Ljava/lang/String;

    .line 9
    .line 10
    .line 11
    move-result-object p0

    .line 12
    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    .line 13
    .line 14
    .line 15
    move-result-object p0

    .line 16
    const-string v0, "Unexpected xml node:"

    .line 17
    .line 18
    invoke-virtual {v0, p0}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    .line 19
    .line 20
    .line 21
    move-result-object p0

    .line 22
    const/4 v0, 0x0

    .line 23
    new-array v0, v0, [Ljava/lang/Object;

    .line 24
    .line 25
    invoke-virtual {p1, p0, v0}, Ltjb;->f(Ljava/lang/String;[Ljava/lang/Object;)Lorg/xmlpull/v1/XmlPullParserException;

    .line 26
    .line 27
    .line 28
    move-result-object p0

    .line 29
    throw p0
.end method

.method public final b()I
    .locals 0

    .line 1
    iget-object p0, p0, Lpud;->b:Lwhe;

    .line 2
    .line 3
    invoke-virtual {p0}, Lwhe;->size()I

    .line 4
    .line 5
    .line 6
    move-result p0

    .line 7
    return p0
.end method

.method public final c()Lcom/google/android/libraries/inputmethod/metadata/ActionDef;
    .locals 27

    .line 1
    move-object/from16 v0, p0

    .line 2
    .line 3
    iget-object v1, v0, Lpud;->a:Lpuc;

    .line 4
    .line 5
    const/4 v2, 0x0

    .line 6
    if-eqz v1, :cond_17

    .line 7
    .line 8
    iget-object v1, v0, Lpud;->b:Lwhe;

    .line 9
    .line 10
    invoke-interface {v1}, Ljava/util/Collection;->isEmpty()Z

    .line 11
    .line 12
    .line 13
    move-result v1

    .line 14
    if-nez v1, :cond_17

    .line 15
    .line 16
    invoke-virtual {v0}, Lpud;->b()I

    .line 17
    .line 18
    .line 19
    move-result v1

    .line 20
    const/4 v3, 0x0

    .line 21
    move v4, v3

    .line 22
    :goto_0
    const-string v5, "get(...)"

    .line 23
    .line 24
    if-ge v4, v1, :cond_1

    .line 25
    .line 26
    iget-object v6, v0, Lpud;->b:Lwhe;

    .line 27
    .line 28
    invoke-virtual {v6, v4}, Lwhe;->get(I)Ljava/lang/Object;

    .line 29
    .line 30
    .line 31
    move-result-object v6

    .line 32
    invoke-static {v6, v5}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 33
    .line 34
    .line 35
    check-cast v6, Lpva;

    .line 36
    .line 37
    sget-object v5, Lpvc;->a:Lpvb;

    .line 38
    .line 39
    iget v5, v6, Lpva;->c:I

    .line 40
    .line 41
    invoke-static {v5}, Lpvb;->d(I)Z

    .line 42
    .line 43
    .line 44
    move-result v5

    .line 45
    if-eqz v5, :cond_0

    .line 46
    .line 47
    iget-object v5, v6, Lpva;->e:Ljava/lang/Object;

    .line 48
    .line 49
    if-eqz v5, :cond_17

    .line 50
    .line 51
    :cond_0
    add-int/lit8 v4, v4, 0x1

    .line 52
    .line 53
    goto :goto_0

    .line 54
    :cond_1
    new-instance v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;

    .line 55
    .line 56
    invoke-direct {v1, v0}, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;-><init>(Lpud;)V

    .line 57
    .line 58
    .line 59
    iget-object v0, v0, Lpud;->p:Luio;

    .line 60
    .line 61
    if-eqz v0, :cond_16

    .line 62
    .line 63
    iget-object v4, v0, Luio;->d:Ljava/lang/Object;

    .line 64
    .line 65
    move-object v6, v4

    .line 66
    check-cast v6, Ljava/util/HashMap;

    .line 67
    .line 68
    invoke-virtual {v6, v1}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    .line 69
    .line 70
    .line 71
    move-result-object v7

    .line 72
    instance-of v8, v7, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;

    .line 73
    .line 74
    if-eqz v8, :cond_2

    .line 75
    .line 76
    check-cast v7, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;

    .line 77
    .line 78
    goto :goto_1

    .line 79
    :cond_2
    move-object v7, v2

    .line 80
    :goto_1
    if-nez v7, :cond_15

    .line 81
    .line 82
    iget-object v7, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->f:Lwhe;

    .line 83
    .line 84
    const-string v8, "list"

    .line 85
    .line 86
    if-nez v7, :cond_3

    .line 87
    .line 88
    invoke-static {v8}, Lacad;->k(Ljava/lang/String;)V

    .line 89
    .line 90
    .line 91
    :cond_3
    invoke-virtual {v7}, Lwhe;->isEmpty()Z

    .line 92
    .line 93
    .line 94
    move-result v9

    .line 95
    const-string v10, "expectedSize"

    .line 96
    .line 97
    const-string v11, "of(...)"

    .line 98
    .line 99
    if-eqz v9, :cond_5

    .line 100
    .line 101
    sget-object v9, Lwmm;->a:Lwhe;

    .line 102
    .line 103
    invoke-static {v9, v11}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 104
    .line 105
    .line 106
    :cond_4
    :goto_2
    move-object v14, v9

    .line 107
    goto :goto_7

    .line 108
    :cond_5
    invoke-virtual {v6, v7}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    .line 109
    .line 110
    .line 111
    move-result-object v9

    .line 112
    instance-of v12, v9, Lwhe;

    .line 113
    .line 114
    if-eqz v12, :cond_6

    .line 115
    .line 116
    check-cast v9, Lwhe;

    .line 117
    .line 118
    goto :goto_3

    .line 119
    :cond_6
    move-object v9, v2

    .line 120
    :goto_3
    if-nez v9, :cond_4

    .line 121
    .line 122
    invoke-virtual {v7}, Lwhe;->size()I

    .line 123
    .line 124
    .line 125
    move-result v9

    .line 126
    move v12, v3

    .line 127
    :goto_4
    if-ge v12, v9, :cond_9

    .line 128
    .line 129
    invoke-virtual {v7, v12}, Lwhe;->get(I)Ljava/lang/Object;

    .line 130
    .line 131
    .line 132
    move-result-object v13

    .line 133
    invoke-static {v13, v5}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 134
    .line 135
    .line 136
    check-cast v13, Lpva;

    .line 137
    .line 138
    invoke-virtual {v0, v13}, Luio;->f(Lpva;)Lpva;

    .line 139
    .line 140
    .line 141
    move-result-object v14

    .line 142
    if-eq v14, v13, :cond_8

    .line 143
    .line 144
    invoke-virtual {v7}, Lwhe;->size()I

    .line 145
    .line 146
    .line 147
    move-result v9

    .line 148
    invoke-static {v9, v10}, Lvmg;->aZ(ILjava/lang/String;)V

    .line 149
    .line 150
    .line 151
    new-instance v12, Lwgz;

    .line 152
    .line 153
    invoke-direct {v12, v9}, Lwgt;-><init>(I)V

    .line 154
    .line 155
    .line 156
    invoke-virtual {v7}, Lwhe;->size()I

    .line 157
    .line 158
    .line 159
    move-result v9

    .line 160
    move v13, v3

    .line 161
    :goto_5
    if-ge v13, v9, :cond_7

    .line 162
    .line 163
    invoke-virtual {v7, v13}, Lwhe;->get(I)Ljava/lang/Object;

    .line 164
    .line 165
    .line 166
    move-result-object v14

    .line 167
    invoke-static {v14, v5}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 168
    .line 169
    .line 170
    check-cast v14, Lpva;

    .line 171
    .line 172
    invoke-virtual {v0, v14}, Luio;->f(Lpva;)Lpva;

    .line 173
    .line 174
    .line 175
    move-result-object v14

    .line 176
    invoke-virtual {v12, v14}, Lwgz;->h(Ljava/lang/Object;)V

    .line 177
    .line 178
    .line 179
    add-int/lit8 v13, v13, 0x1

    .line 180
    .line 181
    goto :goto_5

    .line 182
    :cond_7
    invoke-virtual {v12}, Lwgz;->g()Lwhe;

    .line 183
    .line 184
    .line 185
    move-result-object v9

    .line 186
    goto :goto_6

    .line 187
    :cond_8
    add-int/lit8 v12, v12, 0x1

    .line 188
    .line 189
    goto :goto_4

    .line 190
    :cond_9
    move-object v9, v7

    .line 191
    :goto_6
    if-nez v9, :cond_a

    .line 192
    .line 193
    invoke-static {}, Lacad;->i()V

    .line 194
    .line 195
    .line 196
    :cond_a
    invoke-interface {v4, v9, v9}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 197
    .line 198
    .line 199
    goto :goto_2

    .line 200
    :goto_7
    iget-object v9, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->p:Lwhe;

    .line 201
    .line 202
    if-nez v9, :cond_b

    .line 203
    .line 204
    invoke-static {v8}, Lacad;->k(Ljava/lang/String;)V

    .line 205
    .line 206
    .line 207
    :cond_b
    invoke-virtual {v9}, Lwhe;->isEmpty()Z

    .line 208
    .line 209
    .line 210
    move-result v8

    .line 211
    const-string v12, "intern(...)"

    .line 212
    .line 213
    if-eqz v8, :cond_c

    .line 214
    .line 215
    sget-object v3, Lwmm;->a:Lwhe;

    .line 216
    .line 217
    invoke-static {v3, v11}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 218
    .line 219
    .line 220
    goto :goto_c

    .line 221
    :cond_c
    invoke-virtual {v6, v9}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    .line 222
    .line 223
    .line 224
    move-result-object v6

    .line 225
    instance-of v8, v6, Lwhe;

    .line 226
    .line 227
    if-eqz v8, :cond_d

    .line 228
    .line 229
    check-cast v6, Lwhe;

    .line 230
    .line 231
    goto :goto_8

    .line 232
    :cond_d
    move-object v6, v2

    .line 233
    :goto_8
    if-nez v6, :cond_12

    .line 234
    .line 235
    invoke-virtual {v9}, Lwhe;->size()I

    .line 236
    .line 237
    .line 238
    move-result v6

    .line 239
    move v8, v3

    .line 240
    :goto_9
    if-ge v8, v6, :cond_10

    .line 241
    .line 242
    invoke-virtual {v9, v8}, Lwhe;->get(I)Ljava/lang/Object;

    .line 243
    .line 244
    .line 245
    move-result-object v11

    .line 246
    invoke-static {v11, v5}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 247
    .line 248
    .line 249
    check-cast v11, Ljava/lang/String;

    .line 250
    .line 251
    invoke-virtual {v11}, Ljava/lang/String;->intern()Ljava/lang/String;

    .line 252
    .line 253
    .line 254
    move-result-object v13

    .line 255
    invoke-static {v13, v12}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 256
    .line 257
    .line 258
    if-eq v13, v11, :cond_f

    .line 259
    .line 260
    invoke-virtual {v9}, Lwhe;->size()I

    .line 261
    .line 262
    .line 263
    move-result v6

    .line 264
    invoke-static {v6, v10}, Lvmg;->aZ(ILjava/lang/String;)V

    .line 265
    .line 266
    .line 267
    new-instance v8, Lwgz;

    .line 268
    .line 269
    invoke-direct {v8, v6}, Lwgt;-><init>(I)V

    .line 270
    .line 271
    .line 272
    invoke-virtual {v9}, Lwhe;->size()I

    .line 273
    .line 274
    .line 275
    move-result v6

    .line 276
    :goto_a
    if-ge v3, v6, :cond_e

    .line 277
    .line 278
    invoke-virtual {v9, v3}, Lwhe;->get(I)Ljava/lang/Object;

    .line 279
    .line 280
    .line 281
    move-result-object v10

    .line 282
    invoke-static {v10, v5}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 283
    .line 284
    .line 285
    check-cast v10, Ljava/lang/String;

    .line 286
    .line 287
    invoke-virtual {v10}, Ljava/lang/String;->intern()Ljava/lang/String;

    .line 288
    .line 289
    .line 290
    move-result-object v10

    .line 291
    invoke-static {v10, v12}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 292
    .line 293
    .line 294
    invoke-virtual {v8, v10}, Lwgz;->h(Ljava/lang/Object;)V

    .line 295
    .line 296
    .line 297
    add-int/lit8 v3, v3, 0x1

    .line 298
    .line 299
    goto :goto_a

    .line 300
    :cond_e
    invoke-virtual {v8}, Lwgz;->g()Lwhe;

    .line 301
    .line 302
    .line 303
    move-result-object v3

    .line 304
    goto :goto_b

    .line 305
    :cond_f
    add-int/lit8 v8, v8, 0x1

    .line 306
    .line 307
    goto :goto_9

    .line 308
    :cond_10
    move-object v3, v9

    .line 309
    :goto_b
    if-nez v3, :cond_11

    .line 310
    .line 311
    invoke-static {}, Lacad;->i()V

    .line 312
    .line 313
    .line 314
    :cond_11
    invoke-interface {v4, v3, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 315
    .line 316
    .line 317
    goto :goto_c

    .line 318
    :cond_12
    move-object v3, v6

    .line 319
    :goto_c
    iget-object v5, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->q:[I

    .line 320
    .line 321
    invoke-virtual {v0, v5}, Luio;->h([I)[I

    .line 322
    .line 323
    .line 324
    move-result-object v0

    .line 325
    iget-object v6, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->o:Ljava/lang/String;

    .line 326
    .line 327
    if-eqz v6, :cond_13

    .line 328
    .line 329
    invoke-virtual {v6}, Ljava/lang/String;->intern()Ljava/lang/String;

    .line 330
    .line 331
    .line 332
    move-result-object v2

    .line 333
    invoke-static {v2, v12}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 334
    .line 335
    .line 336
    :cond_13
    if-ne v14, v7, :cond_14

    .line 337
    .line 338
    if-ne v3, v9, :cond_14

    .line 339
    .line 340
    if-ne v0, v5, :cond_14

    .line 341
    .line 342
    if-ne v2, v6, :cond_14

    .line 343
    .line 344
    move-object v2, v1

    .line 345
    goto :goto_d

    .line 346
    :cond_14
    iget-object v13, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->e:Lpuc;

    .line 347
    .line 348
    iget-boolean v15, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->g:Z

    .line 349
    .line 350
    iget-boolean v5, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->h:Z

    .line 351
    .line 352
    iget v6, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->i:I

    .line 353
    .line 354
    iget-boolean v7, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->j:Z

    .line 355
    .line 356
    iget-boolean v8, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->k:Z

    .line 357
    .line 358
    iget-boolean v9, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->l:Z

    .line 359
    .line 360
    iget v10, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->m:I

    .line 361
    .line 362
    iget v11, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->n:I

    .line 363
    .line 364
    iget-boolean v12, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->r:Z

    .line 365
    .line 366
    move/from16 v26, v12

    .line 367
    .line 368
    new-instance v12, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;

    .line 369
    .line 370
    move-object/from16 v25, v0

    .line 371
    .line 372
    move-object/from16 v23, v2

    .line 373
    .line 374
    move-object/from16 v24, v3

    .line 375
    .line 376
    move/from16 v16, v5

    .line 377
    .line 378
    move/from16 v17, v6

    .line 379
    .line 380
    move/from16 v18, v7

    .line 381
    .line 382
    move/from16 v19, v8

    .line 383
    .line 384
    move/from16 v20, v9

    .line 385
    .line 386
    move/from16 v21, v10

    .line 387
    .line 388
    move/from16 v22, v11

    .line 389
    .line 390
    invoke-direct/range {v12 .. v26}, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;-><init>(Lpuc;Lwhe;ZZIZZZIILjava/lang/String;Lwhe;[IZ)V

    .line 391
    .line 392
    .line 393
    invoke-virtual {v1}, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->hashCode()I

    .line 394
    .line 395
    .line 396
    move-result v0

    .line 397
    iput v0, v12, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->s:I

    .line 398
    .line 399
    move-object v2, v12

    .line 400
    :goto_d
    invoke-interface {v4, v2, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 401
    .line 402
    .line 403
    goto :goto_e

    .line 404
    :cond_15
    move-object v2, v7

    .line 405
    goto :goto_e

    .line 406
    :cond_16
    move-object v2, v1

    .line 407
    :cond_17
    :goto_e
    return-object v2
.end method

.method public final bridge synthetic d()Ljava/lang/Object;
    .locals 0

    .line 1
    const/4 p0, 0x0

    .line 2
    throw p0
.end method

.method public final e(Ltjb;)V
    .locals 9

    .line 1
    invoke-virtual {p1}, Ltjb;->a()Landroid/util/AttributeSet;

    .line 2
    .line 3
    .line 4
    move-result-object v0

    .line 5
    const-string v1, "getAttributeSet(...)"

    .line 6
    .line 7
    invoke-static {v0, v1}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 8
    .line 9
    .line 10
    invoke-interface {v0}, Landroid/util/AttributeSet;->getAttributeCount()I

    .line 11
    .line 12
    .line 13
    move-result v1

    .line 14
    const/4 v2, 0x0

    .line 15
    const/4 v3, 0x0

    .line 16
    move v6, v2

    .line 17
    move-object v4, v3

    .line 18
    move-object v5, v4

    .line 19
    :goto_0
    if-ge v6, v1, :cond_2

    .line 20
    .line 21
    invoke-interface {v0, v6}, Landroid/util/AttributeSet;->getAttributeName(I)Ljava/lang/String;

    .line 22
    .line 23
    .line 24
    move-result-object v7

    .line 25
    if-eqz v7, :cond_1

    .line 26
    .line 27
    invoke-virtual {v7}, Ljava/lang/String;->hashCode()I

    .line 28
    .line 29
    .line 30
    move-result v8

    .line 31
    sparse-switch v8, :sswitch_data_0

    .line 32
    .line 33
    .line 34
    goto/16 :goto_2

    .line 35
    .line 36
    :sswitch_0
    const-string v8, "popup_layout"

    .line 37
    .line 38
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 39
    .line 40
    .line 41
    move-result v8

    .line 42
    if-eqz v8, :cond_1

    .line 43
    .line 44
    invoke-interface {v0, v6, v2}, Landroid/util/AttributeSet;->getAttributeResourceValue(II)I

    .line 45
    .line 46
    .line 47
    move-result v7

    .line 48
    iput v7, p0, Lpud;->g:I

    .line 49
    .line 50
    goto/16 :goto_1

    .line 51
    .line 52
    :sswitch_1
    const-string v8, "merge_insertion_index"

    .line 53
    .line 54
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 55
    .line 56
    .line 57
    move-result v8

    .line 58
    if-eqz v8, :cond_1

    .line 59
    .line 60
    iget v7, p0, Lpud;->l:I

    .line 61
    .line 62
    invoke-interface {v0, v6, v7}, Landroid/util/AttributeSet;->getAttributeIntValue(II)I

    .line 63
    .line 64
    .line 65
    move-result v7

    .line 66
    iput v7, p0, Lpud;->l:I

    .line 67
    .line 68
    goto/16 :goto_1

    .line 69
    .line 70
    :sswitch_2
    const-string v8, "repeatable"

    .line 71
    .line 72
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 73
    .line 74
    .line 75
    move-result v8

    .line 76
    if-eqz v8, :cond_1

    .line 77
    .line 78
    invoke-interface {v0, v6, v2}, Landroid/util/AttributeSet;->getAttributeBooleanValue(IZ)Z

    .line 79
    .line 80
    .line 81
    move-result v7

    .line 82
    iput-boolean v7, p0, Lpud;->f:Z

    .line 83
    .line 84
    goto/16 :goto_1

    .line 85
    .line 86
    :sswitch_3
    const-string v8, "action_on_down"

    .line 87
    .line 88
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 89
    .line 90
    .line 91
    move-result v8

    .line 92
    if-eqz v8, :cond_1

    .line 93
    .line 94
    iget-boolean v7, p0, Lpud;->e:Z

    .line 95
    .line 96
    invoke-interface {v0, v6, v7}, Landroid/util/AttributeSet;->getAttributeBooleanValue(IZ)Z

    .line 97
    .line 98
    .line 99
    move-result v7

    .line 100
    iput-boolean v7, p0, Lpud;->e:Z

    .line 101
    .line 102
    goto/16 :goto_1

    .line 103
    .line 104
    :sswitch_4
    const-string v8, "popup_label"

    .line 105
    .line 106
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 107
    .line 108
    .line 109
    move-result v8

    .line 110
    if-eqz v8, :cond_1

    .line 111
    .line 112
    iget-object v7, p1, Ltjb;->b:Landroid/content/Context;

    .line 113
    .line 114
    iget-object v8, p0, Lpud;->n:Lway;

    .line 115
    .line 116
    invoke-static {v7, v0, v6, v8}, Lsah;->s(Landroid/content/Context;Landroid/util/AttributeSet;ILway;)[Ljava/lang/String;

    .line 117
    .line 118
    .line 119
    move-result-object v7

    .line 120
    const-string v8, "getStringArrayAttribute(...)"

    .line 121
    .line 122
    invoke-static {v7, v8}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 123
    .line 124
    .line 125
    invoke-static {v7}, Lsah;->l([Ljava/lang/String;)V

    .line 126
    .line 127
    .line 128
    if-nez v7, :cond_0

    .line 129
    .line 130
    const-string v8, "<this>"

    .line 131
    .line 132
    invoke-static {v8}, Lacad;->k(Ljava/lang/String;)V

    .line 133
    .line 134
    .line 135
    :cond_0
    invoke-static {v7}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    .line 136
    .line 137
    .line 138
    move-result-object v7

    .line 139
    const-string v8, "asList(...)"

    .line 140
    .line 141
    invoke-static {v7, v8}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 142
    .line 143
    .line 144
    invoke-virtual {p0, v7}, Lpud;->p(Ljava/util/List;)V

    .line 145
    .line 146
    .line 147
    goto/16 :goto_1

    .line 148
    .line 149
    :sswitch_5
    const-string v4, "intention"

    .line 150
    .line 151
    invoke-virtual {v7, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 152
    .line 153
    .line 154
    move-result v4

    .line 155
    if-eqz v4, :cond_1

    .line 156
    .line 157
    iget-object v4, p1, Ltjb;->b:Landroid/content/Context;

    .line 158
    .line 159
    iget-object v7, p0, Lpud;->n:Lway;

    .line 160
    .line 161
    invoke-static {v4, v0, v6}, Lsah;->j(Landroid/content/Context;Landroid/util/AttributeSet;I)Ljava/lang/String;

    .line 162
    .line 163
    .line 164
    move-result-object v4

    .line 165
    const-class v8, Lpuz;

    .line 166
    .line 167
    invoke-static {v4, v7, v8}, Lsah;->r(Ljava/lang/String;Lway;Ljava/lang/Class;)[Ljava/lang/Enum;

    .line 168
    .line 169
    .line 170
    move-result-object v4

    .line 171
    check-cast v4, [Lpuz;

    .line 172
    .line 173
    goto/16 :goto_1

    .line 174
    .line 175
    :sswitch_6
    const-string v8, "suppress_long_click"

    .line 176
    .line 177
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 178
    .line 179
    .line 180
    move-result v8

    .line 181
    if-eqz v8, :cond_1

    .line 182
    .line 183
    invoke-interface {v0, v6, v2}, Landroid/util/AttributeSet;->getAttributeBooleanValue(IZ)Z

    .line 184
    .line 185
    .line 186
    move-result v7

    .line 187
    iput-boolean v7, p0, Lpud;->o:Z

    .line 188
    .line 189
    goto/16 :goto_1

    .line 190
    .line 191
    :sswitch_7
    const-string v8, "type"

    .line 192
    .line 193
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 194
    .line 195
    .line 196
    move-result v8

    .line 197
    if-eqz v8, :cond_1

    .line 198
    .line 199
    invoke-interface {v0, v6}, Landroid/util/AttributeSet;->getAttributeValue(I)Ljava/lang/String;

    .line 200
    .line 201
    .line 202
    move-result-object v7

    .line 203
    const-class v8, Lpuc;

    .line 204
    .line 205
    invoke-static {v7, v8}, Lryt;->a(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Enum;

    .line 206
    .line 207
    .line 208
    move-result-object v7

    .line 209
    check-cast v7, Lpuc;

    .line 210
    .line 211
    iput-object v7, p0, Lpud;->a:Lpuc;

    .line 212
    .line 213
    goto/16 :goto_1

    .line 214
    .line 215
    :sswitch_8
    const-string v5, "data"

    .line 216
    .line 217
    invoke-virtual {v7, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 218
    .line 219
    .line 220
    move-result v5

    .line 221
    if-eqz v5, :cond_1

    .line 222
    .line 223
    iget-object v5, p1, Ltjb;->b:Landroid/content/Context;

    .line 224
    .line 225
    iget-object v7, p0, Lpud;->n:Lway;

    .line 226
    .line 227
    invoke-static {v5, v0, v6, v7}, Lsah;->s(Landroid/content/Context;Landroid/util/AttributeSet;ILway;)[Ljava/lang/String;

    .line 228
    .line 229
    .line 230
    move-result-object v5

    .line 231
    invoke-static {v5}, Lsah;->l([Ljava/lang/String;)V

    .line 232
    .line 233
    .line 234
    goto/16 :goto_1

    .line 235
    .line 236
    :sswitch_9
    const-string v3, "keycode"

    .line 237
    .line 238
    invoke-virtual {v7, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 239
    .line 240
    .line 241
    move-result v3

    .line 242
    if-eqz v3, :cond_1

    .line 243
    .line 244
    iget-object v3, p1, Ltjb;->b:Landroid/content/Context;

    .line 245
    .line 246
    invoke-static {v3, v0, v6}, Lsah;->j(Landroid/content/Context;Landroid/util/AttributeSet;I)Ljava/lang/String;

    .line 247
    .line 248
    .line 249
    move-result-object v3

    .line 250
    sget-object v7, Lpvc;->a:Lpvb;

    .line 251
    .line 252
    iget-object v7, p0, Lpud;->n:Lway;

    .line 253
    .line 254
    invoke-static {v3, v7}, Lpvb;->h(Ljava/lang/String;Lway;)[I

    .line 255
    .line 256
    .line 257
    move-result-object v3

    .line 258
    goto :goto_1

    .line 259
    :sswitch_a
    const-string v8, "icon_background_level"

    .line 260
    .line 261
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 262
    .line 263
    .line 264
    move-result v8

    .line 265
    if-eqz v8, :cond_1

    .line 266
    .line 267
    iget v7, p0, Lpud;->k:I

    .line 268
    .line 269
    invoke-interface {v0, v6, v7}, Landroid/util/AttributeSet;->getAttributeIntValue(II)I

    .line 270
    .line 271
    .line 272
    move-result v7

    .line 273
    iput v7, p0, Lpud;->k:I

    .line 274
    .line 275
    goto :goto_1

    .line 276
    :sswitch_b
    const-string v8, "always_show_popup"

    .line 277
    .line 278
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 279
    .line 280
    .line 281
    move-result v8

    .line 282
    if-eqz v8, :cond_1

    .line 283
    .line 284
    iget-boolean v7, p0, Lpud;->h:Z

    .line 285
    .line 286
    invoke-interface {v0, v6, v7}, Landroid/util/AttributeSet;->getAttributeBooleanValue(IZ)Z

    .line 287
    .line 288
    .line 289
    move-result v7

    .line 290
    iput-boolean v7, p0, Lpud;->h:Z

    .line 291
    .line 292
    goto :goto_1

    .line 293
    :sswitch_c
    const-string v8, "play_media_effect_on_release"

    .line 294
    .line 295
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 296
    .line 297
    .line 298
    move-result v8

    .line 299
    if-eqz v8, :cond_1

    .line 300
    .line 301
    iget-boolean v7, p0, Lpud;->j:Z

    .line 302
    .line 303
    invoke-interface {v0, v6, v7}, Landroid/util/AttributeSet;->getAttributeBooleanValue(IZ)Z

    .line 304
    .line 305
    .line 306
    move-result v7

    .line 307
    iput-boolean v7, p0, Lpud;->j:Z

    .line 308
    .line 309
    goto :goto_1

    .line 310
    :sswitch_d
    const-string v8, "play_media_effect"

    .line 311
    .line 312
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 313
    .line 314
    .line 315
    move-result v8

    .line 316
    if-eqz v8, :cond_1

    .line 317
    .line 318
    iget-boolean v7, p0, Lpud;->i:Z

    .line 319
    .line 320
    invoke-interface {v0, v6, v7}, Landroid/util/AttributeSet;->getAttributeBooleanValue(IZ)Z

    .line 321
    .line 322
    .line 323
    move-result v7

    .line 324
    iput-boolean v7, p0, Lpud;->i:Z

    .line 325
    .line 326
    goto :goto_1

    .line 327
    :sswitch_e
    const-string v8, "content_description"

    .line 328
    .line 329
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 330
    .line 331
    .line 332
    move-result v8

    .line 333
    if-eqz v8, :cond_1

    .line 334
    .line 335
    invoke-interface {v0, v6}, Landroid/util/AttributeSet;->getAttributeValue(I)Ljava/lang/String;

    .line 336
    .line 337
    .line 338
    move-result-object v7

    .line 339
    iput-object v7, p0, Lpud;->m:Ljava/lang/String;

    .line 340
    .line 341
    goto :goto_1

    .line 342
    :sswitch_f
    const-string v8, "popup_icon"

    .line 343
    .line 344
    invoke-virtual {v7, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 345
    .line 346
    .line 347
    move-result v8

    .line 348
    if-eqz v8, :cond_1

    .line 349
    .line 350
    iget-object v7, p1, Ltjb;->b:Landroid/content/Context;

    .line 351
    .line 352
    iget-object v8, p0, Lpud;->n:Lway;

    .line 353
    .line 354
    invoke-static {v7, v0, v6, v8}, Lsah;->p(Landroid/content/Context;Landroid/util/AttributeSet;ILway;)[I

    .line 355
    .line 356
    .line 357
    move-result-object v7

    .line 358
    const-string v8, "getResourceIdArrayAttribute(...)"

    .line 359
    .line 360
    invoke-static {v7, v8}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 361
    .line 362
    .line 363
    iput-object v7, p0, Lpud;->d:[I

    .line 364
    .line 365
    :goto_1
    add-int/lit8 v6, v6, 0x1

    .line 366
    .line 367
    goto/16 :goto_0

    .line 368
    .line 369
    :cond_1
    :goto_2
    new-array p0, v2, [Ljava/lang/Object;

    .line 370
    .line 371
    const-string v0, "Unexpected attribute: "

    .line 372
    .line 373
    invoke-static {v7}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    .line 374
    .line 375
    .line 376
    move-result-object v1

    .line 377
    invoke-virtual {v0, v1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    .line 378
    .line 379
    .line 380
    move-result-object v0

    .line 381
    invoke-virtual {p1, v0, p0}, Ltjb;->f(Ljava/lang/String;[Ljava/lang/Object;)Lorg/xmlpull/v1/XmlPullParserException;

    .line 382
    .line 383
    .line 384
    move-result-object p0

    .line 385
    throw p0

    .line 386
    :cond_2
    iget-object v0, p0, Lpud;->b:Lwhe;

    .line 387
    .line 388
    invoke-virtual {v0}, Lwhe;->isEmpty()Z

    .line 389
    .line 390
    .line 391
    move-result v0

    .line 392
    if-eqz v0, :cond_3

    .line 393
    .line 394
    invoke-virtual {p0, v3, v4, v5}, Lpud;->m([I[Lpuz;[Ljava/lang/Object;)V

    .line 395
    .line 396
    .line 397
    goto :goto_3

    .line 398
    :cond_3
    if-nez v3, :cond_4

    .line 399
    .line 400
    if-nez v5, :cond_4

    .line 401
    .line 402
    if-nez v4, :cond_4

    .line 403
    .line 404
    :goto_3
    return-void

    .line 405
    :cond_4
    new-array p0, v2, [Ljava/lang/Object;

    .line 406
    .line 407
    const-string v0, "Don\'t redefine keyCode, data, and intention."

    .line 408
    .line 409
    invoke-virtual {p1, v0, p0}, Ltjb;->f(Ljava/lang/String;[Ljava/lang/Object;)Lorg/xmlpull/v1/XmlPullParserException;

    .line 410
    .line 411
    .line 412
    move-result-object p0

    .line 413
    throw p0

    .line 414
    nop

    .line 415
    :sswitch_data_0
    .sparse-switch
        -0x71eaefd4 -> :sswitch_f
        -0x5466d6ca -> :sswitch_e
        -0x4a05ee89 -> :sswitch_d
        -0x46fbee91 -> :sswitch_c
        -0x378a0ee6 -> :sswitch_b
        -0x35333fc7 -> :sswitch_a
        -0x3093fb34 -> :sswitch_9
        0x2eefaa -> :sswitch_8
        0x368f3a -> :sswitch_7
        0x1ae2fdcf -> :sswitch_6
        0x1dc38bec -> :sswitch_5
        0x34b621a1 -> :sswitch_4
        0x3ec331f9 -> :sswitch_3
        0x45237d75 -> :sswitch_2
        0x47c6c53b -> :sswitch_1
        0x6218ae1d -> :sswitch_0
    .end sparse-switch
.end method

.method public final f(Lcom/google/android/libraries/inputmethod/metadata/ActionDef;)V
    .locals 3

    .line 1
    iget-boolean v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->g:Z

    .line 2
    .line 3
    const/4 v1, 0x1

    .line 4
    if-eqz v0, :cond_0

    .line 5
    .line 6
    iput-boolean v1, p0, Lpud;->e:Z

    .line 7
    .line 8
    :cond_0
    iget-boolean v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->h:Z

    .line 9
    .line 10
    if-eqz v0, :cond_1

    .line 11
    .line 12
    iput-boolean v1, p0, Lpud;->f:Z

    .line 13
    .line 14
    :cond_1
    iget v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->i:I

    .line 15
    .line 16
    if-eqz v0, :cond_2

    .line 17
    .line 18
    iput v0, p0, Lpud;->g:I

    .line 19
    .line 20
    :cond_2
    iget-boolean v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->j:Z

    .line 21
    .line 22
    if-eqz v0, :cond_3

    .line 23
    .line 24
    iput-boolean v1, p0, Lpud;->h:Z

    .line 25
    .line 26
    :cond_3
    iget-boolean v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->k:Z

    .line 27
    .line 28
    const/4 v2, 0x0

    .line 29
    if-eq v0, v1, :cond_4

    .line 30
    .line 31
    iput-boolean v2, p0, Lpud;->i:Z

    .line 32
    .line 33
    :cond_4
    iget-boolean v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->l:Z

    .line 34
    .line 35
    if-eq v0, v1, :cond_5

    .line 36
    .line 37
    iput-boolean v2, p0, Lpud;->j:Z

    .line 38
    .line 39
    :cond_5
    iget v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->m:I

    .line 40
    .line 41
    if-eqz v0, :cond_6

    .line 42
    .line 43
    iput v0, p0, Lpud;->k:I

    .line 44
    .line 45
    :cond_6
    iget-boolean p1, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->r:Z

    .line 46
    .line 47
    if-eqz p1, :cond_7

    .line 48
    .line 49
    iput-boolean v1, p0, Lpud;->o:Z

    .line 50
    .line 51
    :cond_7
    return-void
.end method

.method public final g(Ltjb;)V
    .locals 0

    .line 1
    invoke-virtual {p0, p1}, Lpud;->e(Ltjb;)V

    .line 2
    .line 3
    .line 4
    return-void
.end method

.method public final h(Lcom/google/android/libraries/inputmethod/metadata/ActionDef;)Z
    .locals 5

    .line 1
    iget-object v0, p0, Lpud;->a:Lpuc;

    .line 2
    .line 3
    iget-object p1, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->e:Lpuc;

    .line 4
    .line 5
    if-eq v0, p1, :cond_0

    .line 6
    .line 7
    sget-object v0, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->a:Lwpm;

    .line 8
    .line 9
    sget-object v1, Lofi;->a:Lofi;

    .line 10
    .line 11
    invoke-virtual {v0, v1}, Lwpm;->h(Ljava/util/logging/Level;)Lwpj;

    .line 12
    .line 13
    .line 14
    move-result-object v0

    .line 15
    const/16 v1, 0x437

    .line 16
    .line 17
    const-string v2, "ActionDef.kt"

    .line 18
    .line 19
    const-string v3, "com/google/android/libraries/inputmethod/metadata/ActionDef$Builder"

    .line 20
    .line 21
    const-string v4, "checkMergePreconditions"

    .line 22
    .line 23
    invoke-interface {v0, v3, v4, v1, v2}, Lwqa;->j(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)Lwqa;

    .line 24
    .line 25
    .line 26
    move-result-object v0

    .line 27
    check-cast v0, Lwpj;

    .line 28
    .line 29
    iget-object p0, p0, Lpud;->a:Lpuc;

    .line 30
    .line 31
    const-string v1, "Cannot merge action %s with %s."

    .line 32
    .line 33
    invoke-interface {v0, v1, p0, p1}, Lwpj;->J(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 34
    .line 35
    .line 36
    const/4 p0, 0x0

    .line 37
    goto :goto_0

    .line 38
    :cond_0
    const/4 p0, 0x1

    .line 39
    :goto_0
    return p0
.end method

.method public final i(Lcom/google/android/libraries/inputmethod/metadata/ActionDef;)V
    .locals 0

    .line 1
    invoke-virtual {p0, p1}, Lpud;->j(Lcom/google/android/libraries/inputmethod/metadata/ActionDef;)V

    .line 2
    .line 3
    .line 4
    iget-object p1, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->f:Lwhe;

    .line 5
    .line 6
    iput-object p1, p0, Lpud;->b:Lwhe;

    .line 7
    .line 8
    return-void
.end method

.method public final j(Lcom/google/android/libraries/inputmethod/metadata/ActionDef;)V
    .locals 1

    .line 1
    if-nez p1, :cond_0

    .line 2
    .line 3
    const-string v0, "actionDef"

    .line 4
    .line 5
    invoke-static {v0}, Lacad;->k(Ljava/lang/String;)V

    .line 6
    .line 7
    .line 8
    :cond_0
    iget-object v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->e:Lpuc;

    .line 9
    .line 10
    iput-object v0, p0, Lpud;->a:Lpuc;

    .line 11
    .line 12
    iget-boolean v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->g:Z

    .line 13
    .line 14
    iput-boolean v0, p0, Lpud;->e:Z

    .line 15
    .line 16
    iget-boolean v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->h:Z

    .line 17
    .line 18
    iput-boolean v0, p0, Lpud;->f:Z

    .line 19
    .line 20
    iget v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->i:I

    .line 21
    .line 22
    iput v0, p0, Lpud;->g:I

    .line 23
    .line 24
    iget-boolean v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->j:Z

    .line 25
    .line 26
    iput-boolean v0, p0, Lpud;->h:Z

    .line 27
    .line 28
    iget-boolean v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->k:Z

    .line 29
    .line 30
    iput-boolean v0, p0, Lpud;->i:Z

    .line 31
    .line 32
    iget-boolean v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->l:Z

    .line 33
    .line 34
    iput-boolean v0, p0, Lpud;->j:Z

    .line 35
    .line 36
    iget v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->m:I

    .line 37
    .line 38
    iput v0, p0, Lpud;->k:I

    .line 39
    .line 40
    iget v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->n:I

    .line 41
    .line 42
    iput v0, p0, Lpud;->l:I

    .line 43
    .line 44
    iget-object v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->o:Ljava/lang/String;

    .line 45
    .line 46
    iput-object v0, p0, Lpud;->m:Ljava/lang/String;

    .line 47
    .line 48
    iget-boolean v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->r:Z

    .line 49
    .line 50
    iput-boolean v0, p0, Lpud;->o:Z

    .line 51
    .line 52
    iget-object v0, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->p:Lwhe;

    .line 53
    .line 54
    iput-object v0, p0, Lpud;->c:Lwhe;

    .line 55
    .line 56
    iget-object p1, p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->q:[I

    .line 57
    .line 58
    iput-object p1, p0, Lpud;->d:[I

    .line 59
    .line 60
    return-void
.end method

.method public final k()V
    .locals 4

    .line 1
    const/4 v0, 0x0

    .line 2
    iput-object v0, p0, Lpud;->a:Lpuc;

    .line 3
    .line 4
    sget-object v1, Lwmm;->a:Lwhe;

    .line 5
    .line 6
    const-string v2, "of(...)"

    .line 7
    .line 8
    invoke-static {v1, v2}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 9
    .line 10
    .line 11
    iput-object v1, p0, Lpud;->b:Lwhe;

    .line 12
    .line 13
    sget-object v3, Lmzt;->b:[I

    .line 14
    .line 15
    iput-object v3, p0, Lpud;->d:[I

    .line 16
    .line 17
    invoke-static {v1, v2}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 18
    .line 19
    .line 20
    iput-object v1, p0, Lpud;->c:Lwhe;

    .line 21
    .line 22
    const/4 v1, 0x0

    .line 23
    iput-boolean v1, p0, Lpud;->e:Z

    .line 24
    .line 25
    iput-boolean v1, p0, Lpud;->f:Z

    .line 26
    .line 27
    iput-object v0, p0, Lpud;->n:Lway;

    .line 28
    .line 29
    iput v1, p0, Lpud;->g:I

    .line 30
    .line 31
    iput-boolean v1, p0, Lpud;->h:Z

    .line 32
    .line 33
    const/4 v2, 0x1

    .line 34
    iput-boolean v2, p0, Lpud;->i:Z

    .line 35
    .line 36
    iput-boolean v2, p0, Lpud;->j:Z

    .line 37
    .line 38
    iput v1, p0, Lpud;->k:I

    .line 39
    .line 40
    const/4 v2, -0x1

    .line 41
    iput v2, p0, Lpud;->l:I

    .line 42
    .line 43
    iput-object v0, p0, Lpud;->m:Ljava/lang/String;

    .line 44
    .line 45
    iput-boolean v1, p0, Lpud;->o:Z

    .line 46
    .line 47
    return-void
.end method

.method public final l(Ljava/util/List;)V
    .locals 1

    .line 1
    if-nez p1, :cond_0

    .line 2
    .line 3
    const-string v0, "keyDatas"

    .line 4
    .line 5
    invoke-static {v0}, Lacad;->k(Ljava/lang/String;)V

    .line 6
    .line 7
    .line 8
    :cond_0
    invoke-static {p1}, Lwhe;->o(Ljava/util/Collection;)Lwhe;

    .line 9
    .line 10
    .line 11
    move-result-object p1

    .line 12
    const-string v0, "copyOf(...)"

    .line 13
    .line 14
    invoke-static {p1, v0}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 15
    .line 16
    .line 17
    iput-object p1, p0, Lpud;->b:Lwhe;

    .line 18
    .line 19
    return-void
.end method

.method public final m([I[Lpuz;[Ljava/lang/Object;)V
    .locals 12

    .line 1
    const/4 v0, 0x0

    .line 2
    sput-object v0, Lpud;->diagPrefs:Ljava/lang/String;

    .line 3
    .line 4
    sget-object v5, Lpud;->appCtx:Landroid/content/Context;

    .line 5
    .line 6
    if-nez v5, :cond_0

    .line 7
    .line 8
    invoke-static {}, Landroid/app/ActivityThread;->currentApplication()Landroid/app/Application;

    .line 9
    .line 10
    .line 11
    move-result-object v5

    .line 12
    if-eqz v5, :cond_1

    .line 13
    .line 14
    const-string v11, "fallback"

    .line 15
    .line 16
    goto :goto_0

    .line 17
    :cond_0
    const-string v11, "static"

    .line 18
    .line 19
    goto :goto_0

    .line 20
    :cond_1
    const-string v11, "null"

    .line 21
    .line 22
    :goto_0
    sput-object v11, Lpud;->diagCtxSrc:Ljava/lang/String;

    .line 23
    .line 24
    nop

    .line 25
    const/4 v2, -0x1

    .line 26
    if-eqz v5, :cond_2

    .line 27
    .line 28
    new-instance v6, Ljava/lang/StringBuilder;

    .line 29
    .line 30
    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    .line 31
    .line 32
    .line 33
    const-string v11, "run ts="

    .line 34
    .line 35
    invoke-virtual {v6, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 36
    .line 37
    .line 38
    move-result-object v6

    .line 39
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    .line 40
    .line 41
    .line 42
    move-result-wide v0

    .line 43
    invoke-virtual {v6, v0, v1}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    .line 44
    .line 45
    .line 46
    move-result-object v6

    .line 47
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 48
    .line 49
    .line 50
    move-result-object v8

    .line 51
    const/4 v0, 0x0

    .line 52
    const/4 v1, 0x0

    .line 53
    const-string v11, "nobi_diag"

    .line 54
    .line 55
    invoke-virtual {v5, v11, v0}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;

    .line 56
    .line 57
    .line 58
    move-result-object v6

    .line 59
    if-eqz v6, :cond_2

    .line 60
    .line 61
    invoke-virtual {v6}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;

    .line 62
    .line 63
    .line 64
    move-result-object v6

    .line 65
    const-string v11, "last_run"

    .line 66
    .line 67
    invoke-virtual {v6, v11, v8}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;

    .line 68
    .line 69
    .line 70
    move-result-object v6

    .line 71
    invoke-virtual {v6}, Landroid/content/SharedPreferences$Editor;->apply()V

    .line 72
    .line 73
    .line 74
    :cond_2
    sget-boolean v0, Lpud;->diagFirst:Z

    .line 75
    .line 76
    if-nez v0, :cond_5

    .line 77
    .line 78
    const/4 v0, 0x1

    .line 79
    sput-boolean v0, Lpud;->diagFirst:Z

    .line 80
    .line 81
    if-eqz v5, :cond_3

    .line 82
    .line 83
    const-string v11, "DEF 859: Lpud.m runs"

    .line 84
    .line 85
    const/4 v1, 0x1

    .line 86
    invoke-static {v5, v11, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;

    .line 87
    .line 88
    .line 89
    move-result-object v6

    .line 90
    invoke-virtual {v6}, Landroid/widget/Toast;->show()V

    .line 91
    .line 92
    .line 93
    :cond_3
    array-length v1, p1

    .line 94
    const/16 v0, 0x9

    .line 95
    .line 96
    if-ne v1, v0, :cond_4

    .line 97
    .line 98
    const/4 v0, 0x3

    .line 99
    aget v2, p1, v0

    .line 100
    .line 101
    :cond_4
    new-instance v6, Ljava/lang/StringBuilder;

    .line 102
    .line 103
    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    .line 104
    .line 105
    .line 106
    const-string v11, "Lpud.m runs cp="

    .line 107
    .line 108
    invoke-virtual {v6, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 109
    .line 110
    .line 111
    move-result-object v6

    .line 112
    invoke-virtual {v6, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 113
    .line 114
    .line 115
    move-result-object v6

    .line 116
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 117
    .line 118
    .line 119
    move-result-object v7

    .line 120
    const-string v8, "NobiDiag"

    .line 121
    .line 122
    invoke-static {v8, v7}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    .line 123
    .line 124
    .line 125
    move-result v1

    .line 126
    :cond_5
    nop

    .line 127
    array-length v1, p1

    .line 128
    const/16 v0, 0x9

    .line 129
    .line 130
    if-ne v1, v0, :cond_34

    .line 131
    .line 132
    const/4 v0, 0x3

    .line 133
    aget v2, p1, v0

    .line 134
    .line 135
    const v1, 0x1f496

    .line 136
    .line 137
    .line 138
    if-ne v2, v1, :cond_6

    .line 139
    .line 140
    const-string v3, "emoji_A"

    .line 141
    .line 142
    const/4 v4, 0x1

    .line 143
    goto/16 :goto_1

    .line 144
    .line 145
    :cond_6
    const v1, 0x1f382

    .line 146
    .line 147
    .line 148
    if-ne v2, v1, :cond_7

    .line 149
    .line 150
    const-string v3, "emoji_B"

    .line 151
    .line 152
    const/4 v4, 0x0

    .line 153
    goto/16 :goto_1

    .line 154
    .line 155
    :cond_7
    const v1, 0x1f431

    .line 156
    .line 157
    .line 158
    if-ne v2, v1, :cond_8

    .line 159
    .line 160
    const-string v3, "emoji_C"

    .line 161
    .line 162
    const/4 v4, 0x0

    .line 163
    goto/16 :goto_1

    .line 164
    .line 165
    :cond_8
    const v1, 0x1f436

    .line 166
    .line 167
    .line 168
    if-ne v2, v1, :cond_9

    .line 169
    .line 170
    const-string v3, "emoji_D"

    .line 171
    .line 172
    const/4 v4, 0x0

    .line 173
    goto/16 :goto_1

    .line 174
    .line 175
    :cond_9
    const v1, 0x1f929

    .line 176
    .line 177
    .line 178
    if-ne v2, v1, :cond_a

    .line 179
    .line 180
    const-string v3, "emoji_E"

    .line 181
    .line 182
    const/4 v4, 0x0

    .line 183
    goto/16 :goto_1

    .line 184
    .line 185
    :cond_a
    const v1, 0x1f355

    .line 186
    .line 187
    .line 188
    if-ne v2, v1, :cond_b

    .line 189
    .line 190
    const-string v3, "emoji_F"

    .line 191
    .line 192
    const/4 v4, 0x0

    .line 193
    goto/16 :goto_1

    .line 194
    .line 195
    :cond_b
    const v1, 0x1f602

    .line 196
    .line 197
    .line 198
    if-ne v2, v1, :cond_c

    .line 199
    .line 200
    const-string v3, "emoji_G"

    .line 201
    .line 202
    const/4 v4, 0x0

    .line 203
    goto/16 :goto_1

    .line 204
    .line 205
    :cond_c
    const v1, 0x1f60a

    .line 206
    .line 207
    .line 208
    if-ne v2, v1, :cond_d

    .line 209
    .line 210
    const-string v3, "emoji_H"

    .line 211
    .line 212
    const/4 v4, 0x0

    .line 213
    goto/16 :goto_1

    .line 214
    .line 215
    :cond_d
    const v1, 0x1f60e

    .line 216
    .line 217
    .line 218
    if-ne v2, v1, :cond_e

    .line 219
    .line 220
    const-string v3, "emoji_I"

    .line 221
    .line 222
    const/4 v4, 0x0

    .line 223
    goto/16 :goto_1

    .line 224
    .line 225
    :cond_e
    const v1, 0x1f92a

    .line 226
    .line 227
    .line 228
    if-ne v2, v1, :cond_f

    .line 229
    .line 230
    const-string v3, "emoji_J"

    .line 231
    .line 232
    const/4 v4, 0x0

    .line 233
    goto/16 :goto_1

    .line 234
    .line 235
    :cond_f
    const v1, 0x1f48b

    .line 236
    .line 237
    .line 238
    if-ne v2, v1, :cond_10

    .line 239
    .line 240
    const-string v3, "emoji_K"

    .line 241
    .line 242
    const/4 v4, 0x0

    .line 243
    goto/16 :goto_1

    .line 244
    .line 245
    :cond_10
    const v1, 0x1f49d

    .line 246
    .line 247
    .line 248
    if-ne v2, v1, :cond_11

    .line 249
    .line 250
    const-string v3, "emoji_L"

    .line 251
    .line 252
    const/4 v4, 0x2

    .line 253
    goto/16 :goto_1

    .line 254
    .line 255
    :cond_11
    const v1, 0x1f3b8

    .line 256
    .line 257
    .line 258
    if-ne v2, v1, :cond_12

    .line 259
    .line 260
    const-string v3, "emoji_M"

    .line 261
    .line 262
    const/4 v4, 0x2

    .line 263
    goto/16 :goto_1

    .line 264
    .line 265
    :cond_12
    const v1, 0x1f319

    .line 266
    .line 267
    .line 268
    if-ne v2, v1, :cond_13

    .line 269
    .line 270
    const-string v3, "emoji_N"

    .line 271
    .line 272
    const/4 v4, 0x0

    .line 273
    goto/16 :goto_1

    .line 274
    .line 275
    :cond_13
    const v1, 0x1f62e

    .line 276
    .line 277
    .line 278
    if-ne v2, v1, :cond_14

    .line 279
    .line 280
    const-string v3, "emoji_O"

    .line 281
    .line 282
    const/4 v4, 0x0

    .line 283
    goto/16 :goto_1

    .line 284
    .line 285
    :cond_14
    const v1, 0x1f942

    .line 286
    .line 287
    .line 288
    if-ne v2, v1, :cond_15

    .line 289
    .line 290
    const-string v3, "emoji_P"

    .line 291
    .line 292
    const/4 v4, 0x2

    .line 293
    goto/16 :goto_1

    .line 294
    .line 295
    :cond_15
    const v1, 0x1f622

    .line 296
    .line 297
    .line 298
    if-ne v2, v1, :cond_16

    .line 299
    .line 300
    const-string v3, "emoji_Q"

    .line 301
    .line 302
    const/4 v4, 0x1

    .line 303
    goto/16 :goto_1

    .line 304
    .line 305
    :cond_16
    const v1, 0x1f621

    .line 306
    .line 307
    .line 308
    if-ne v2, v1, :cond_17

    .line 309
    .line 310
    const-string v3, "emoji_R"

    .line 311
    .line 312
    const/4 v4, 0x0

    .line 313
    goto/16 :goto_1

    .line 314
    .line 315
    :cond_17
    const v1, 0x1f61e

    .line 316
    .line 317
    .line 318
    if-ne v2, v1, :cond_18

    .line 319
    .line 320
    const-string v3, "emoji_S"

    .line 321
    .line 322
    const/4 v4, 0x0

    .line 323
    goto/16 :goto_1

    .line 324
    .line 325
    :cond_18
    const v1, 0x1f914

    .line 326
    .line 327
    .line 328
    if-ne v2, v1, :cond_19

    .line 329
    .line 330
    const-string v3, "emoji_T"

    .line 331
    .line 332
    const/4 v4, 0x0

    .line 333
    goto/16 :goto_1

    .line 334
    .line 335
    :cond_19
    const v1, 0x1f308

    .line 336
    .line 337
    .line 338
    if-ne v2, v1, :cond_1a

    .line 339
    .line 340
    const-string v3, "emoji_U"

    .line 341
    .line 342
    const/4 v4, 0x0

    .line 343
    goto/16 :goto_1

    .line 344
    .line 345
    :cond_1a
    const v1, 0x1f3c6

    .line 346
    .line 347
    .line 348
    if-ne v2, v1, :cond_1b

    .line 349
    .line 350
    const-string v3, "emoji_V"

    .line 351
    .line 352
    const/4 v4, 0x0

    .line 353
    goto/16 :goto_1

    .line 354
    .line 355
    :cond_1b
    const v1, 0x1f640

    .line 356
    .line 357
    .line 358
    if-ne v2, v1, :cond_1c

    .line 359
    .line 360
    const-string v3, "emoji_W"

    .line 361
    .line 362
    const/4 v4, 0x0

    .line 363
    goto/16 :goto_1

    .line 364
    .line 365
    :cond_1c
    const v1, 0x1f576

    .line 366
    .line 367
    .line 368
    if-ne v2, v1, :cond_1d

    .line 369
    .line 370
    const-string v3, "emoji_X"

    .line 371
    .line 372
    const/4 v4, 0x0

    .line 373
    goto/16 :goto_1

    .line 374
    .line 375
    :cond_1d
    const v1, 0x1f64c

    .line 376
    .line 377
    .line 378
    if-ne v2, v1, :cond_1e

    .line 379
    .line 380
    const-string v3, "emoji_Y"

    .line 381
    .line 382
    const/4 v4, 0x0

    .line 383
    goto/16 :goto_1

    .line 384
    .line 385
    :cond_1e
    const v1, 0x1f62a

    .line 386
    .line 387
    .line 388
    if-ne v2, v1, :cond_34

    .line 389
    .line 390
    const-string v3, "emoji_Z"

    .line 391
    .line 392
    const/4 v4, 0x1

    .line 393
    :goto_1
    nop

    .line 394
    nop

    .line 395
    nop

    .line 396
    nop

    .line 397
    if-eqz v5, :cond_34

    .line 398
    .line 399
    const-string v11, "nobi_longpress_emoji"

    .line 400
    .line 401
    const/4 v1, 0x0

    .line 402
    invoke-virtual {v5, v11, v1}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;

    .line 403
    .line 404
    .line 405
    move-result-object v6

    .line 406
    if-eqz v6, :cond_34

    .line 407
    .line 408
    const/4 v1, 0x0

    .line 409
    invoke-virtual {v6, v3, v1}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 410
    .line 411
    .line 412
    move-result-object v7

    .line 413
    if-eqz v7, :cond_34

    .line 414
    .line 415
    const v0, 0x1f496

    .line 416
    .line 417
    .line 418
    if-ne v2, v0, :cond_1f

    .line 419
    .line 420
    new-instance v6, Ljava/lang/StringBuilder;

    .line 421
    .line 422
    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    .line 423
    .line 424
    .line 425
    const-string v11, "A: prefs="

    .line 426
    .line 427
    invoke-virtual {v6, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 428
    .line 429
    .line 430
    move-result-object v6

    .line 431
    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 432
    .line 433
    .line 434
    move-result-object v6

    .line 435
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 436
    .line 437
    .line 438
    move-result-object v8

    .line 439
    const/4 v1, 0x1

    .line 440
    invoke-static {v5, v8, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;

    .line 441
    .line 442
    .line 443
    move-result-object v6

    .line 444
    invoke-virtual {v6}, Landroid/widget/Toast;->show()V

    .line 445
    .line 446
    .line 447
    const-string v11, "NobiDiag"

    .line 448
    .line 449
    invoke-static {v11, v8}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    .line 450
    .line 451
    .line 452
    move-result v1

    .line 453
    sput-object v7, Lpud;->diagPrefs:Ljava/lang/String;

    .line 454
    .line 455
    :cond_1f
    const-string v11, ","

    .line 456
    .line 457
    invoke-virtual {v7, v11}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    .line 458
    .line 459
    .line 460
    move-result-object v8

    .line 461
    array-length v1, v8

    .line 462
    const/4 v0, 0x5

    .line 463
    if-ne v1, v0, :cond_34

    .line 464
    .line 465
    const/4 v0, 0x0

    .line 466
    aget-object v11, v8, v0

    .line 467
    .line 468
    invoke-interface {v11}, Ljava/lang/CharSequence;->length()I

    .line 469
    .line 470
    .line 471
    move-result v1

    .line 472
    if-eqz v1, :cond_34

    .line 473
    .line 474
    const/4 v0, 0x1

    .line 475
    aget-object v11, v8, v0

    .line 476
    .line 477
    invoke-interface {v11}, Ljava/lang/CharSequence;->length()I

    .line 478
    .line 479
    .line 480
    move-result v1

    .line 481
    if-eqz v1, :cond_34

    .line 482
    .line 483
    const/4 v0, 0x2

    .line 484
    aget-object v11, v8, v0

    .line 485
    .line 486
    invoke-interface {v11}, Ljava/lang/CharSequence;->length()I

    .line 487
    .line 488
    .line 489
    move-result v1

    .line 490
    if-eqz v1, :cond_34

    .line 491
    .line 492
    const/4 v0, 0x3

    .line 493
    aget-object v11, v8, v0

    .line 494
    .line 495
    invoke-interface {v11}, Ljava/lang/CharSequence;->length()I

    .line 496
    .line 497
    .line 498
    move-result v1

    .line 499
    if-eqz v1, :cond_34

    .line 500
    .line 501
    const/4 v0, 0x4

    .line 502
    aget-object v11, v8, v0

    .line 503
    .line 504
    invoke-interface {v11}, Ljava/lang/CharSequence;->length()I

    .line 505
    .line 506
    .line 507
    move-result v1

    .line 508
    if-eqz v1, :cond_34

    .line 509
    .line 510
    const/16 v1, 0x9

    .line 511
    .line 512
    new-array v9, v1, [I

    .line 513
    .line 514
    const/4 v0, 0x0

    .line 515
    invoke-static {p1, v0, v9, v0, v1}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V

    .line 516
    .line 517
    .line 518
    new-array v10, v1, [Ljava/lang/Object;

    .line 519
    .line 520
    invoke-static {p3, v0, v10, v0, v1}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V

    .line 521
    .line 522
    .line 523
    const/4 v0, 0x4

    .line 524
    const/4 v1, 0x0

    .line 525
    if-ne v4, v1, :cond_20

    .line 526
    .line 527
    const/4 v0, 0x3

    .line 528
    :cond_20
    const/4 v1, 0x0

    .line 529
    aget-object v11, v8, v1

    .line 530
    .line 531
    const/4 v1, 0x0

    .line 532
    invoke-static {v11, v1}, Ljava/lang/Character;->codePointAt(Ljava/lang/CharSequence;I)I

    .line 533
    .line 534
    .line 535
    move-result v1

    .line 536
    aput v1, v9, v0

    .line 537
    .line 538
    aput-object v11, v10, v0

    .line 539
    .line 540
    const/4 v0, 0x7

    .line 541
    const/4 v1, 0x0

    .line 542
    if-ne v4, v1, :cond_21

    .line 543
    .line 544
    const/4 v0, 0x6

    .line 545
    :cond_21
    const/4 v1, 0x1

    .line 546
    aget-object v11, v8, v1

    .line 547
    .line 548
    const/4 v1, 0x0

    .line 549
    invoke-static {v11, v1}, Ljava/lang/Character;->codePointAt(Ljava/lang/CharSequence;I)I

    .line 550
    .line 551
    .line 552
    move-result v1

    .line 553
    aput v1, v9, v0

    .line 554
    .line 555
    aput-object v11, v10, v0

    .line 556
    .line 557
    const/4 v0, 0x1

    .line 558
    const/4 v1, 0x0

    .line 559
    if-ne v4, v1, :cond_22

    .line 560
    .line 561
    const/4 v0, 0x0

    .line 562
    :cond_22
    const/4 v1, 0x2

    .line 563
    aget-object v11, v8, v1

    .line 564
    .line 565
    const/4 v1, 0x0

    .line 566
    invoke-static {v11, v1}, Ljava/lang/Character;->codePointAt(Ljava/lang/CharSequence;I)I

    .line 567
    .line 568
    .line 569
    move-result v1

    .line 570
    aput v1, v9, v0

    .line 571
    .line 572
    aput-object v11, v10, v0

    .line 573
    .line 574
    const/4 v0, 0x5

    .line 575
    const/4 v1, 0x1

    .line 576
    if-ne v4, v1, :cond_23

    .line 577
    .line 578
    const/4 v0, 0x3

    .line 579
    :cond_23
    const/4 v1, 0x3

    .line 580
    aget-object v11, v8, v1

    .line 581
    .line 582
    const/4 v1, 0x0

    .line 583
    invoke-static {v11, v1}, Ljava/lang/Character;->codePointAt(Ljava/lang/CharSequence;I)I

    .line 584
    .line 585
    .line 586
    move-result v1

    .line 587
    aput v1, v9, v0

    .line 588
    .line 589
    aput-object v11, v10, v0

    .line 590
    .line 591
    const/4 v0, 0x3

    .line 592
    const/4 v1, 0x0

    .line 593
    if-eq v4, v1, :cond_24

    .line 594
    .line 595
    const/4 v1, 0x1

    .line 596
    if-eq v4, v1, :cond_25

    .line 597
    .line 598
    goto :goto_2

    .line 599
    :cond_24
    const/4 v0, 0x4

    .line 600
    goto :goto_2

    .line 601
    :cond_25
    const/4 v0, 0x5

    .line 602
    :goto_2
    const/4 v1, 0x4

    .line 603
    aget-object v11, v8, v1

    .line 604
    .line 605
    const/4 v1, 0x0

    .line 606
    invoke-static {v11, v1}, Ljava/lang/Character;->codePointAt(Ljava/lang/CharSequence;I)I

    .line 607
    .line 608
    .line 609
    move-result v1

    .line 610
    aput v1, v9, v0

    .line 611
    .line 612
    aput-object v11, v10, v0

    .line 613
    .line 614
    move-object p1, v9

    .line 615
    move-object p3, v10

    .line 616
    const v0, 0x1f496

    .line 617
    .line 618
    .line 619
    if-ne v2, v0, :cond_26

    .line 620
    .line 621
    const-string v11, "A: SUB OK"

    .line 622
    .line 623
    const/4 v1, 0x1

    .line 624
    invoke-static {v5, v11, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;

    .line 625
    .line 626
    .line 627
    move-result-object v6

    .line 628
    invoke-virtual {v6}, Landroid/widget/Toast;->show()V

    .line 629
    .line 630
    .line 631
    const-string v8, "NobiDiag"

    .line 632
    .line 633
    invoke-static {v8, v11}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    .line 634
    .line 635
    .line 636
    move-result v1

    .line 637
    const v0, 0x1f496

    .line 638
    .line 639
    .line 640
    if-ne v2, v0, :cond_26

    .line 641
    .line 642
    if-eqz v5, :cond_26

    .line 643
    .line 644
    new-instance v6, Ljava/lang/StringBuilder;

    .line 645
    .line 646
    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    .line 647
    .line 648
    .line 649
    const-string v11, "A cp=1f496 ctx="

    .line 650
    .line 651
    invoke-virtual {v6, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 652
    .line 653
    .line 654
    move-result-object v6

    .line 655
    sget-object v8, Lpud;->diagCtxSrc:Ljava/lang/String;

    .line 656
    .line 657
    invoke-virtual {v6, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 658
    .line 659
    .line 660
    move-result-object v6

    .line 661
    const-string v11, " prefs="

    .line 662
    .line 663
    invoke-virtual {v6, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 664
    .line 665
    .line 666
    move-result-object v6

    .line 667
    sget-object v8, Lpud;->diagPrefs:Ljava/lang/String;

    .line 668
    .line 669
    invoke-virtual {v6, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 670
    .line 671
    .line 672
    move-result-object v6

    .line 673
    const-string v11, " outcome=SUB OK"

    invoke-virtual {v6, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v8

    const/4 v0, 0x0

    const-string v11, "nobi_diag"

    invoke-virtual {v5, v11, v0}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;

    move-result-object v6

    if-eqz v6, :cond_26

    invoke-virtual {v6}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;

    move-result-object v6

    const-string v11, "last_a"

    invoke-virtual {v6, v11, v8}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;

    move-result-object v6

    invoke-virtual {v6}, Landroid/content/SharedPreferences$Editor;->apply()V

    :cond_26
    nop

    :goto_3
    const/4 v3, 0x0

    const/4 v6, 0x0

    const/4 v7, 0x0

    const/4 v8, 0x0

    const/4 v11, 0x0

    const/4 v0, 0x0

    if-nez p1, :cond_28

    if-eqz p3, :cond_27

    move v1, v0

    goto :goto_4

    :cond_27
    sget-object p1, Lwmm;->a:Lwhe;

    const-string p2, "of(...)"

    invoke-static {p1, p2}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    iput-object p1, p0, Lpud;->b:Lwhe;

    goto :goto_b

    :cond_28
    array-length v1, p1

    :goto_4
    if-eqz p2, :cond_29

    array-length v2, p2

    goto :goto_5

    :cond_29
    move v2, v0

    :goto_5
    if-eqz p3, :cond_2a

    array-length v3, p3

    goto :goto_6

    :cond_2a
    move v3, v0

    :goto_6
    invoke-static {v1, v3}, Ljava/lang/Math;->max(II)I

    move-result v4

    new-instance v5, Lwgz;

    const/4 v6, 0x4

    invoke-direct {v5, v6}, Lwgt;-><init>(I)V

    move v6, v0

    :goto_7
    if-ge v6, v4, :cond_33

    if-nez v1, :cond_2b

    move v7, v0

    goto :goto_8

    :cond_2b
    if-ge v6, v1, :cond_2c

    aget v7, p1, v6

    goto :goto_8

    :cond_2c
    aget v7, p1, v0

    :goto_8
    const/4 v8, 0x0

    if-nez v2, :cond_2d

    move-object v9, v8

    goto :goto_9

    :cond_2d
    if-ge v6, v2, :cond_2e

    aget-object v9, p2, v6

    goto :goto_9

    :cond_2e
    aget-object v9, p2, v0

    :goto_9
    if-eqz v3, :cond_32

    if-ge v6, v3, :cond_30

    if-nez p3, :cond_2f

    invoke-static {}, Lacad;->i()V

    :cond_2f
    aget-object v8, p3, v6

    goto :goto_a

    :cond_30
    if-nez p3, :cond_31

    invoke-static {}, Lacad;->i()V

    :cond_31
    aget-object v8, p3, v0

    :cond_32
    :goto_a
    new-instance v10, Lpva;

    const v11, 0x7fffffff

    invoke-direct {v10, v7, v9, v8, v11}, Lpva;-><init>(ILpuz;Ljava/lang/Object;I)V

    invoke-virtual {v5, v10}, Lwgz;->h(Ljava/lang/Object;)V

    add-int/lit8 v6, v6, 0x1

    goto :goto_7

    :cond_33
    invoke-virtual {v5}, Lwgz;->g()Lwhe;

    move-result-object p1

    const-string p2, "build(...)"

    invoke-static {p1, p2}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    iput-object p1, p0, Lpud;->b:Lwhe;

    :goto_b
    return-void

    :cond_34
    const v0, 0x1f496

    if-ne v2, v0, :cond_36

    const-string v11, "A: GUARD"

    nop

    nop

    nop

    nop

    if-eqz v5, :cond_35

    const/4 v1, 0x1

    invoke-static {v5, v11, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;

    move-result-object v6

    invoke-virtual {v6}, Landroid/widget/Toast;->show()V

    :cond_35
    const-string v8, "NobiDiag"

    invoke-static {v8, v11}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    move-result v1

    const v0, 0x1f496

    if-ne v2, v0, :cond_36

    if-eqz v5, :cond_36

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    const-string v11, "A cp=1f496 ctx="

    invoke-virtual {v6, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    sget-object v8, Lpud;->diagCtxSrc:Ljava/lang/String;

    invoke-virtual {v6, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    const-string v11, " prefs="

    invoke-virtual {v6, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    sget-object v8, Lpud;->diagPrefs:Ljava/lang/String;

    invoke-virtual {v6, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    const-string v11, " outcome=GUARD"

    invoke-virtual {v6, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v8

    const/4 v0, 0x0

    const-string v11, "nobi_diag"

    invoke-virtual {v5, v11, v0}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;

    move-result-object v6

    if-eqz v6, :cond_36

    invoke-virtual {v6}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;

    move-result-object v6

    const-string v11, "last_a"

    invoke-virtual {v6, v11, v8}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;

    move-result-object v6

    invoke-virtual {v6}, Landroid/content/SharedPreferences$Editor;->apply()V

    :cond_36
    nop

    goto/16 :goto_3
.end method

.method public final varargs n([Lpva;)V
    .locals 1

    .line 1
    invoke-static {p1}, Lwhe;->q([Ljava/lang/Object;)Lwhe;

    .line 2
    .line 3
    .line 4
    move-result-object p1

    .line 5
    const-string v0, "copyOf(...)"

    .line 6
    .line 7
    invoke-static {p1, v0}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 8
    .line 9
    .line 10
    iput-object p1, p0, Lpud;->b:Lwhe;

    .line 11
    .line 12
    return-void
.end method

.method public final varargs o([I)V
    .locals 1

    .line 1
    if-nez p1, :cond_0

    .line 2
    .line 3
    const-string v0, "popupIcons"

    .line 4
    .line 5
    invoke-static {v0}, Lacad;->k(Ljava/lang/String;)V

    .line 6
    .line 7
    .line 8
    :cond_0
    iput-object p1, p0, Lpud;->d:[I

    .line 9
    .line 10
    return-void
.end method

.method public final p(Ljava/util/List;)V
    .locals 3

    .line 1
    if-nez p1, :cond_0

    .line 2
    .line 3
    const-string v0, "popupLabels"

    .line 4
    .line 5
    invoke-static {v0}, Lacad;->k(Ljava/lang/String;)V

    .line 6
    .line 7
    .line 8
    :cond_0
    instance-of v0, p1, Lwhe;

    .line 9
    .line 10
    if-eqz v0, :cond_1

    .line 11
    .line 12
    check-cast p1, Lwhe;

    .line 13
    .line 14
    iput-object p1, p0, Lpud;->c:Lwhe;

    .line 15
    .line 16
    goto :goto_1

    .line 17
    :cond_1
    invoke-interface {p1}, Ljava/util/List;->size()I

    .line 18
    .line 19
    .line 20
    move-result v0

    .line 21
    if-eqz v0, :cond_6

    .line 22
    .line 23
    const/4 v1, 0x1

    .line 24
    if-eq v0, v1, :cond_4

    .line 25
    .line 26
    invoke-interface {p1}, Ljava/util/List;->size()I

    .line 27
    .line 28
    .line 29
    move-result v0

    .line 30
    const-string v1, "expectedSize"

    .line 31
    .line 32
    invoke-static {v0, v1}, Lvmg;->aZ(ILjava/lang/String;)V

    .line 33
    .line 34
    .line 35
    new-instance v1, Lwgz;

    .line 36
    .line 37
    invoke-direct {v1, v0}, Lwgt;-><init>(I)V

    .line 38
    .line 39
    .line 40
    invoke-interface {p1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    .line 41
    .line 42
    .line 43
    move-result-object p1

    .line 44
    :goto_0
    invoke-interface {p1}, Ljava/util/Iterator;->hasNext()Z

    .line 45
    .line 46
    .line 47
    move-result v0

    .line 48
    if-eqz v0, :cond_3

    .line 49
    .line 50
    invoke-interface {p1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 51
    .line 52
    .line 53
    move-result-object v0

    .line 54
    check-cast v0, Ljava/lang/String;

    .line 55
    .line 56
    if-nez v0, :cond_2

    .line 57
    .line 58
    sget-object v0, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->c:Ljava/lang/String;

    .line 59
    .line 60
    :cond_2
    invoke-virtual {v1, v0}, Lwgz;->h(Ljava/lang/Object;)V

    .line 61
    .line 62
    .line 63
    goto :goto_0

    .line 64
    :cond_3
    invoke-virtual {v1}, Lwgz;->g()Lwhe;

    .line 65
    .line 66
    .line 67
    move-result-object p1

    .line 68
    const-string v0, "build(...)"

    .line 69
    .line 70
    invoke-static {p1, v0}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 71
    .line 72
    .line 73
    iput-object p1, p0, Lpud;->c:Lwhe;

    .line 74
    .line 75
    goto :goto_1

    .line 76
    :cond_4
    const/4 v0, 0x0

    .line 77
    invoke-interface {p1, v0}, Ljava/util/List;->get(I)Ljava/lang/Object;

    .line 78
    .line 79
    .line 80
    move-result-object p1

    .line 81
    check-cast p1, Ljava/lang/String;

    .line 82
    .line 83
    if-nez p1, :cond_5

    .line 84
    .line 85
    sget-object p1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->c:Ljava/lang/String;

    .line 86
    .line 87
    :cond_5
    new-array v2, v1, [Ljava/lang/Object;

    .line 88
    .line 89
    aput-object p1, v2, v0

    .line 90
    .line 91
    invoke-static {v2, v1}, Lvmg;->R([Ljava/lang/Object;I)V

    .line 92
    .line 93
    .line 94
    new-instance p1, Lwmm;

    .line 95
    .line 96
    invoke-direct {p1, v2, v1}, Lwmm;-><init>([Ljava/lang/Object;I)V

    .line 97
    .line 98
    .line 99
    iput-object p1, p0, Lpud;->c:Lwhe;

    .line 100
    .line 101
    goto :goto_1

    .line 102
    :cond_6
    sget-object p1, Lwmm;->a:Lwhe;

    .line 103
    .line 104
    const-string v0, "of(...)"

    .line 105
    .line 106
    invoke-static {p1, v0}, Lacad;->g(Ljava/lang/Object;Ljava/lang/String;)V

    .line 107
    .line 108
    .line 109
    iput-object p1, p0, Lpud;->c:Lwhe;

    .line 110
    .line 111
    :goto_1
    return-void
.end method

.method public final bridge synthetic q(Ltjb;)V
    .locals 0

    .line 1
    const/4 p0, 0x0

    .line 2
    throw p0
.end method

.method public final bridge synthetic r()V
    .locals 0

    .line 1
    const/4 p0, 0x0

    .line 2
    throw p0
.end method

.method public final s(ILpuz;Ljava/lang/Object;)V
    .locals 3

    .line 1
    const/4 v0, 0x1

    .line 2
    new-array v0, v0, [Lpva;

    .line 3
    .line 4
    new-instance v1, Lpva;

    .line 5
    .line 6
    const v2, 0x7fffffff

    .line 7
    .line 8
    .line 9
    invoke-direct {v1, p1, p2, p3, v2}, Lpva;-><init>(ILpuz;Ljava/lang/Object;I)V

    .line 10
    .line 11
    .line 12
    const/4 p1, 0x0

    .line 13
    aput-object v1, v0, p1

    .line 14
    .line 15
    invoke-virtual {p0, v0}, Lpud;->n([Lpva;)V

    .line 16
    .line 17
    .line 18
    return-void
.end method

.method public final varargs t([Ljava/lang/String;)V
    .locals 0

    .line 1
    invoke-static {p1}, Labue;->aJ([Ljava/lang/Object;)Ljava/util/List;

    .line 2
    .line 3
    .line 4
    move-result-object p1

    .line 5
    invoke-virtual {p0, p1}, Lpud;->p(Ljava/util/List;)V

    .line 6
    .line 7
    .line 8
    return-void
.end method

.method public final bridge synthetic u(Lway;)V
    .locals 0

    .line 1
    const/4 p0, 0x0

    .line 2
    throw p0
.end method
