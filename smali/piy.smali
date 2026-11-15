.class public final synthetic Lpiy;
.super Ljava/lang/Object;
.source "PG"

# interfaces
.implements Ljava/lang/Runnable;


# instance fields
.field private final synthetic $cid:I

.field public final synthetic a:Ljava/lang/Object;


# direct methods
.method public synthetic constructor <init>(Ljava/lang/Object;I)V
    .locals 0

    .line 12
    iput p2, p0, Lpiy;->$cid:I

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lpiy;->a:Ljava/lang/Object;

    return-void
.end method

.method public constructor <init>(Lqck;I[C)V
    .locals 0

    .line 1
    iput p2, p0, Lpiy;->$cid:I

    .line 2
    .line 3
    invoke-static {p1}, Lj$/util/Objects;->requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;

    .line 4
    .line 5
    .line 6
    iput-object p1, p0, Lpiy;->a:Ljava/lang/Object;

    .line 7
    .line 8
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 9
    .line 10
    .line 11
    return-void
.end method


# virtual methods
.method public final run()V
    .locals 9

    .line 1
    iget v0, p0, Lpiy;->$cid:I

    .line 2
    .line 3
    const/4 v1, 0x0

    .line 4
    const/4 v2, 0x1

    .line 5
    const/4 v3, 0x0

    .line 6
    packed-switch v0, :pswitch_data_0

    .line 7
    .line 8
    .line 9
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 10
    .line 11
    check-cast p0, Lqck;

    .line 12
    .line 13
    iget-wide v0, p0, Lqck;->x:J

    .line 14
    .line 15
    invoke-virtual {p0, v0, v1}, Lqck;->s(J)V

    .line 16
    .line 17
    .line 18
    iget-boolean v0, p0, Lqck;->l:Z

    .line 19
    .line 20
    if-eqz v0, :cond_6

    .line 21
    .line 22
    iget-object v0, p0, Lqck;->r:Lqcl;

    .line 23
    .line 24
    invoke-interface {v0, p0}, Lqcl;->j(Lqck;)V

    .line 25
    .line 26
    .line 27
    return-void

    .line 28
    :pswitch_0
    sget v0, Lqck;->F:I

    .line 29
    .line 30
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 31
    .line 32
    check-cast p0, Ljava/lang/ref/WeakReference;

    .line 33
    .line 34
    invoke-virtual {p0}, Ljava/lang/ref/WeakReference;->get()Ljava/lang/Object;

    .line 35
    .line 36
    .line 37
    move-result-object p0

    .line 38
    check-cast p0, Landroid/view/View;

    .line 39
    .line 40
    if-eqz p0, :cond_6

    .line 41
    .line 42
    invoke-virtual {p0, v3}, Landroid/view/View;->setPressed(Z)V

    .line 43
    .line 44
    .line 45
    return-void

    .line 46
    :pswitch_1
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 47
    .line 48
    sget-object v0, Lpuc;->b:Lpuc;

    .line 49
    .line 50
    move-object v1, p0

    .line 51
    check-cast v1, Lqck;

    .line 52
    .line 53
    invoke-virtual {v1, v0}, Lqck;->k(Lpuc;)Lcom/google/android/libraries/inputmethod/metadata/ActionDef;

    .line 54
    .line 55
    .line 56
    move-result-object v2

    .line 57
    if-eqz v2, :cond_6

    .line 58
    .line 59
    iget-object p0, v1, Lqck;->r:Lqcl;

    .line 60
    .line 61
    check-cast p0, Lqch;

    .line 62
    .line 63
    iget-object v3, p0, Lqch;->d:Lqcq;

    .line 64
    .line 65
    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    .line 66
    .line 67
    .line 68
    move-result-wide v6

    .line 69
    invoke-interface {v3}, Lqcq;->n()V

    .line 70
    .line 71
    .line 72
    iget-boolean v5, v2, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->g:Z

    .line 73
    .line 74
    const/4 v4, 0x1

    .line 75
    const/high16 v8, -0x80000000

    .line 76
    .line 77
    const/4 v3, 0x0

    .line 78
    invoke-virtual/range {v1 .. v8}, Lqck;->n(Lcom/google/android/libraries/inputmethod/metadata/ActionDef;ZZZJI)V

    .line 79
    .line 80
    .line 81
    iget-object v2, v1, Lqck;->i:Lpuc;

    .line 82
    .line 83
    if-ne v2, v0, :cond_0

    .line 84
    .line 85
    iget-object p0, p0, Lqch;->o:Lqcm;

    .line 86
    .line 87
    invoke-virtual {p0}, Lqcm;->c()V

    .line 88
    .line 89
    .line 90
    return-void

    .line 91
    :cond_0
    const-wide/16 v2, 0x0

    .line 92
    .line 93
    invoke-virtual {v1, v2, v3}, Lqck;->D(J)V

    .line 94
    .line 95
    .line 96
    return-void

    .line 97
    :pswitch_2
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 98
    .line 99
    move-object v0, p0

    .line 100
    check-cast v0, Lqck;

    .line 101
    .line 102
    invoke-virtual {v0}, Lqck;->I()Z

    .line 103
    .line 104
    .line 105
    move-result p0

    .line 106
    if-eqz p0, :cond_6

    .line 107
    .line 108
    iget-object v1, v0, Lqck;->n:Lcom/google/android/libraries/inputmethod/metadata/ActionDef;

    .line 109
    .line 110
    iget-boolean p0, v1, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->h:Z

    .line 111
    .line 112
    if-eqz p0, :cond_6

    .line 113
    .line 114
    invoke-virtual {v0}, Lqck;->m()Lcom/google/android/libraries/inputmethod/metadata/SoftKeyDef;

    .line 115
    .line 116
    .line 117
    move-result-object v2

    .line 118
    invoke-virtual {v0}, Lqck;->f()J

    .line 119
    .line 120
    .line 121
    move-result-wide v3

    .line 122
    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    .line 123
    .line 124
    .line 125
    move-result-wide v6

    .line 126
    const/4 v5, 0x1

    .line 127
    const/high16 v8, -0x80000000

    .line 128
    .line 129
    invoke-virtual/range {v0 .. v8}, Lqck;->q(Lcom/google/android/libraries/inputmethod/metadata/ActionDef;Lcom/google/android/libraries/inputmethod/metadata/SoftKeyDef;JZJI)V

    .line 130
    .line 131
    .line 132
    invoke-virtual {v0}, Lqck;->m()Lcom/google/android/libraries/inputmethod/metadata/SoftKeyDef;

    .line 133
    .line 134
    .line 135
    move-result-object p0

    .line 136
    if-eqz p0, :cond_6

    .line 137
    .line 138
    iget-object v1, v0, Lqck;->s:Lxkf;

    .line 139
    .line 140
    iget-object v2, v0, Lqck;->z:Ljava/lang/Runnable;

    .line 141
    .line 142
    invoke-virtual {p0}, Lcom/google/android/libraries/inputmethod/metadata/SoftKeyDef;->e()I

    .line 143
    .line 144
    .line 145
    move-result p0

    .line 146
    int-to-long v3, p0

    .line 147
    sget-object p0, Ljava/util/concurrent/TimeUnit;->MILLISECONDS:Ljava/util/concurrent/TimeUnit;

    .line 148
    .line 149
    invoke-interface {v1, v2, v3, v4, p0}, Lxkf;->a(Ljava/lang/Runnable;JLjava/util/concurrent/TimeUnit;)Lxkd;

    .line 150
    .line 151
    .line 152
    move-result-object p0

    .line 153
    iput-object p0, v0, Lqck;->y:Lxkb;

    .line 154
    .line 155
    return-void

    .line 156
    :pswitch_3
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 157
    .line 158
    check-cast p0, Lqai;

    .line 159
    .line 160
    const-string v0, "MIC_TOOLTIP_SHOWED"

    .line 161
    .line 162
    invoke-virtual {p0, v0}, Lqai;->e(Ljava/lang/String;)V

    .line 163
    .line 164
    .line 165
    return-void

    .line 166
    :pswitch_4
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 167
    .line 168
    instance-of v0, p0, Ljava/lang/RuntimeException;

    .line 169
    .line 170
    if-eqz v0, :cond_1

    .line 171
    .line 172
    check-cast p0, Ljava/lang/RuntimeException;

    .line 173
    .line 174
    throw p0

    .line 175
    :cond_1
    new-instance v0, Ljava/lang/IllegalStateException;

    .line 176
    .line 177
    check-cast p0, Ljava/lang/Throwable;

    .line 178
    .line 179
    invoke-direct {v0, p0}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/Throwable;)V

    .line 180
    .line 181
    .line 182
    throw v0

    .line 183
    :pswitch_5
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 184
    .line 185
    check-cast p0, Lptn;

    .line 186
    .line 187
    iput-boolean v3, p0, Lptn;->d:Z

    .line 188
    .line 189
    iput-boolean v2, p0, Lptn;->e:Z

    .line 190
    .line 191
    iget v0, p0, Lptn;->f:I

    .line 192
    .line 193
    add-int/2addr v0, v2

    .line 194
    iput v0, p0, Lptn;->f:I

    .line 195
    .line 196
    sget-object p0, Lptn;->a:Lwpm;

    .line 197
    .line 198
    invoke-virtual {p0}, Lwpc;->b()Lwqa;

    .line 199
    .line 200
    .line 201
    move-result-object p0

    .line 202
    check-cast p0, Lwpj;

    .line 203
    .line 204
    const/16 v0, 0xe3

    .line 205
    .line 206
    const-string v1, "HiddenStateTrimModule.java"

    .line 207
    .line 208
    const-string v4, "com/google/android/libraries/inputmethod/memory/hiddenstate/HiddenStateTrimModule"

    .line 209
    .line 210
    const-string v5, "trim"

    .line 211
    .line 212
    invoke-interface {p0, v4, v5, v0, v1}, Lwpj;->j(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)Lwqa;

    .line 213
    .line 214
    .line 215
    move-result-object p0

    .line 216
    check-cast p0, Lwpj;

    .line 217
    .line 218
    const-string v0, "trim(): no Gboard UI visible for a while, level=%d"

    .line 219
    .line 220
    const/16 v1, 0x28

    .line 221
    .line 222
    invoke-interface {p0, v0, v1}, Lwpj;->u(Ljava/lang/String;I)V

    .line 223
    .line 224
    .line 225
    invoke-static {}, Lpxx;->b()Lpya;

    .line 226
    .line 227
    .line 228
    move-result-object p0

    .line 229
    sget-object v0, Lpyv;->A:Lpyv;

    .line 230
    .line 231
    const/16 v4, 0x3e8

    .line 232
    .line 233
    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 234
    .line 235
    .line 236
    move-result-object v4

    .line 237
    new-array v2, v2, [Ljava/lang/Object;

    .line 238
    .line 239
    aput-object v4, v2, v3

    .line 240
    .line 241
    invoke-interface {p0, v0, v2}, Lpya;->n(Lpyf;[Ljava/lang/Object;)V

    .line 242
    .line 243
    .line 244
    invoke-static {v1}, Lptk;->a(I)V

    .line 245
    .line 246
    .line 247
    return-void

    .line 248
    :pswitch_6
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 249
    .line 250
    check-cast p0, Lprp;

    .line 251
    .line 252
    invoke-virtual {p0}, Lprp;->a()Landroid/os/Bundle;

    .line 253
    .line 254
    .line 255
    move-result-object v0

    .line 256
    invoke-virtual {p0, v0}, Lprp;->c(Landroid/os/Bundle;)V

    .line 257
    .line 258
    .line 259
    return-void

    .line 260
    :pswitch_7
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 261
    .line 262
    move-object v0, p0

    .line 263
    check-cast v0, Lprp;

    .line 264
    .line 265
    iget-object v2, v0, Lprp;->b:Landroid/content/Context;

    .line 266
    .line 267
    check-cast p0, Landroid/content/BroadcastReceiver;

    .line 268
    .line 269
    invoke-virtual {v2, p0}, Landroid/content/Context;->unregisterReceiver(Landroid/content/BroadcastReceiver;)V

    .line 270
    .line 271
    .line 272
    invoke-virtual {v0, v1}, Lprp;->c(Landroid/os/Bundle;)V

    .line 273
    .line 274
    .line 275
    return-void

    .line 276
    :pswitch_8
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 277
    .line 278
    check-cast p0, Lyny;

    .line 279
    .line 280
    iget p0, p0, Lyny;->aw:I

    .line 281
    .line 282
    invoke-static {p0, v2}, Lkpi;->v(IZ)V

    .line 283
    .line 284
    .line 285
    return-void

    .line 286
    :pswitch_9
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 287
    .line 288
    check-cast p0, Lyny;

    .line 289
    .line 290
    iget p0, p0, Lyny;->aw:I

    .line 291
    .line 292
    invoke-static {p0, v3}, Lkpi;->v(IZ)V

    .line 293
    .line 294
    .line 295
    return-void

    .line 296
    :pswitch_a
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 297
    .line 298
    check-cast p0, Lpps;

    .line 299
    .line 300
    invoke-virtual {p0}, Lpps;->getContext()Landroid/content/Context;

    .line 301
    .line 302
    .line 303
    move-result-object v0

    .line 304
    check-cast v0, Lppw;

    .line 305
    .line 306
    iget p0, p0, Lpps;->a:I

    .line 307
    .line 308
    sget-object v1, Lppw;->p:Landroid/util/SparseArray;

    .line 309
    .line 310
    invoke-virtual {v1, p0}, Landroid/util/SparseArray;->get(I)Ljava/lang/Object;

    .line 311
    .line 312
    .line 313
    move-result-object v1

    .line 314
    check-cast v1, Ljava/lang/String;

    .line 315
    .line 316
    invoke-static {v1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    .line 317
    .line 318
    .line 319
    move-result v4

    .line 320
    if-nez v4, :cond_2

    .line 321
    .line 322
    iget-object v4, v0, Lppw;->q:Lpya;

    .line 323
    .line 324
    sget-object v5, Lpyv;->u:Lpyv;

    .line 325
    .line 326
    new-array v6, v2, [Ljava/lang/Object;

    .line 327
    .line 328
    aput-object v1, v6, v3

    .line 329
    .line 330
    invoke-interface {v4, v5, v6}, Lpya;->n(Lpyf;[Ljava/lang/Object;)V

    .line 331
    .line 332
    .line 333
    goto :goto_0

    .line 334
    :cond_2
    sget-object v1, Lppw;->o:Lwpm;

    .line 335
    .line 336
    sget-object v3, Lofi;->a:Lofi;

    .line 337
    .line 338
    invoke-virtual {v1, v3}, Lwpm;->h(Ljava/util/logging/Level;)Lwpj;

    .line 339
    .line 340
    .line 341
    move-result-object v1

    .line 342
    const/16 v3, 0x1b0

    .line 343
    .line 344
    const-string v4, "FirstRunActivity.java"

    .line 345
    .line 346
    const-string v5, "com/google/android/libraries/inputmethod/launcher/firstrun/FirstRunActivity"

    .line 347
    .line 348
    const-string v6, "onSetupFinished"

    .line 349
    .line 350
    invoke-interface {v1, v5, v6, v3, v4}, Lwpj;->j(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)Lwqa;

    .line 351
    .line 352
    .line 353
    move-result-object v1

    .line 354
    check-cast v1, Lwpj;

    .line 355
    .line 356
    invoke-static {p0}, Lrzt;->l(I)Ljava/lang/String;

    .line 357
    .line 358
    .line 359
    move-result-object v3

    .line 360
    const-string v4, "PageId=%s (%s) doesn\'t have valid page name"

    .line 361
    .line 362
    invoke-interface {v1, v4, p0, v3}, Lwpj;->B(Ljava/lang/String;ILjava/lang/Object;)V

    .line 363
    .line 364
    .line 365
    :goto_0
    invoke-virtual {v0, p0}, Lppw;->v(I)I

    .line 366
    .line 367
    .line 368
    move-result p0

    .line 369
    add-int/2addr p0, v2

    .line 370
    iget-object v1, v0, Lppw;->w:[I

    .line 371
    .line 372
    array-length v1, v1

    .line 373
    if-ne p0, v1, :cond_3

    .line 374
    .line 375
    invoke-virtual {v0}, Lppw;->finish()V

    .line 376
    .line 377
    .line 378
    goto :goto_1

    .line 379
    :cond_3
    invoke-virtual {v0, p0}, Lppw;->x(I)V

    .line 380
    .line 381
    .line 382
    goto :goto_1

    .line 383
    :pswitch_b
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 384
    .line 385
    check-cast p0, Lcom/google/android/libraries/inputmethod/launcher/LauncherActivity;

    .line 386
    .line 387
    invoke-virtual {p0}, Lcom/google/android/libraries/inputmethod/launcher/LauncherActivity;->a()V

    .line 388
    .line 389
    .line 390
    iput-object v1, p0, Lcom/google/android/libraries/inputmethod/launcher/LauncherActivity;->c:Lqfi;

    .line 391
    .line 392
    goto :goto_1

    .line 393
    :pswitch_c
    sget v0, Lppc;->a:I

    .line 394
    .line 395
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 396
    .line 397
    check-cast p0, Landroid/widget/AdapterView;

    .line 398
    .line 399
    invoke-virtual {p0, v3}, Landroid/widget/AdapterView;->setSelection(I)V

    .line 400
    .line 401
    .line 402
    goto :goto_1

    .line 403
    :pswitch_d
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 404
    .line 405
    check-cast p0, Loaw;

    .line 406
    .line 407
    invoke-virtual {p0}, Loaw;->ai()Lobj;

    .line 408
    .line 409
    .line 410
    move-result-object p0

    .line 411
    const/16 v0, -0x2714

    .line 412
    .line 413
    sget-object v1, Lpwf;->b:Lpwf;

    .line 414
    .line 415
    invoke-static {v0, v1}, Loak;->e(ILjava/lang/Object;)Loak;

    .line 416
    .line 417
    .line 418
    move-result-object v0

    .line 419
    invoke-virtual {p0, v0}, Lobj;->N(Loak;)V

    .line 420
    .line 421
    .line 422
    goto :goto_1

    .line 423
    :pswitch_e
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 424
    .line 425
    check-cast p0, Lpjy;

    .line 426
    .line 427
    invoke-virtual {p0}, Lpjy;->G()V

    .line 428
    .line 429
    .line 430
    goto :goto_1

    .line 431
    :pswitch_f
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 432
    .line 433
    check-cast p0, Lpjy;

    .line 434
    .line 435
    invoke-virtual {p0}, Lpjy;->G()V

    .line 436
    .line 437
    .line 438
    goto :goto_1

    .line 439
    :pswitch_10
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 440
    .line 441
    check-cast p0, Lpjy;

    .line 442
    .line 443
    invoke-virtual {p0}, Lpjy;->m()V

    .line 444
    .line 445
    .line 446
    goto :goto_1

    .line 447
    :pswitch_11
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 448
    .line 449
    check-cast p0, Lpjh;

    .line 450
    .line 451
    invoke-virtual {p0}, Lpjh;->W()V

    .line 452
    .line 453
    .line 454
    invoke-virtual {p0}, Lpjh;->K()V

    .line 455
    .line 456
    .line 457
    goto :goto_1

    .line 458
    :pswitch_12
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 459
    .line 460
    check-cast p0, Lpjb;

    .line 461
    .line 462
    iget-object p0, p0, Lpjb;->v:Lpjy;

    .line 463
    .line 464
    instance-of v0, p0, Lpiv;

    .line 465
    .line 466
    if-eqz v0, :cond_6

    .line 467
    .line 468
    check-cast p0, Lpiv;

    .line 469
    .line 470
    iget-object v0, p0, Lpiv;->w:Landroid/view/View;

    .line 471
    .line 472
    if-eqz v0, :cond_6

    .line 473
    .line 474
    invoke-virtual {p0}, Lpjy;->s()V

    .line 475
    .line 476
    .line 477
    goto :goto_1

    .line 478
    :pswitch_13
    iget-object p0, p0, Lpiy;->a:Ljava/lang/Object;

    .line 479
    .line 480
    check-cast p0, Lpjb;

    .line 481
    .line 482
    iget-object v0, p0, Lpjb;->o:Lphw;

    .line 483
    .line 484
    if-eqz v0, :cond_4

    .line 485
    .line 486
    iget-object v1, p0, Lpjb;->q:Landroid/view/inputmethod/CursorAnchorInfo;

    .line 487
    .line 488
    iput-object v1, v0, Lphw;->a:Landroid/view/inputmethod/CursorAnchorInfo;

    .line 489
    .line 490
    iput-boolean v3, v0, Lphw;->b:Z

    .line 491
    .line 492
    :cond_4
    iget-boolean v0, p0, Lpjb;->n:Z

    .line 493
    .line 494
    if-nez v0, :cond_5

    .line 495
    .line 496
    invoke-virtual {p0}, Lpjb;->N()V

    .line 497
    .line 498
    .line 499
    :cond_5
    invoke-virtual {p0}, Lpjb;->L()V

    .line 500
    .line 501
    .line 502
    :cond_6
    :goto_1
    return-void

    .line 503
    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_13
        :pswitch_12
        :pswitch_11
        :pswitch_10
        :pswitch_f
        :pswitch_e
        :pswitch_d
        :pswitch_c
        :pswitch_b
        :pswitch_a
        :pswitch_9
        :pswitch_8
        :pswitch_7
        :pswitch_6
        :pswitch_5
        :pswitch_4
        :pswitch_3
        :pswitch_2
        :pswitch_1
        :pswitch_0
    .end packed-switch
.end method
