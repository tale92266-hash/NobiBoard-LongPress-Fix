.class public Lqml;
.super Ljava/lang/Object;
.source "PG"


# instance fields
.field protected a:Z

.field public b:F

.field protected c:Landroid/widget/LinearLayout;

.field public final d:Ljava/util/List;

.field public e:Ljava/util/List;

.field protected final f:Lqmo;

.field private g:I

.field private h:I

.field private i:F

.field private final j:Lrfp;

.field private final k:Ligp;


# direct methods
.method public constructor <init>(Landroid/content/Context;Landroid/util/AttributeSet;Lqmk;)V
    .locals 3

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 2
    .line 3
    .line 4
    const/4 v0, -0x1

    .line 5
    iput v0, p0, Lqml;->g:I

    .line 6
    .line 7
    iput v0, p0, Lqml;->h:I

    .line 8
    .line 9
    const/4 v0, 0x1

    .line 10
    iput-boolean v0, p0, Lqml;->a:Z

    .line 11
    .line 12
    new-instance v0, Ljava/util/ArrayList;

    .line 13
    .line 14
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 15
    .line 16
    .line 17
    iput-object v0, p0, Lqml;->d:Ljava/util/List;

    .line 18
    .line 19
    new-instance v0, Ljava/util/ArrayList;

    .line 20
    .line 21
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 22
    .line 23
    .line 24
    iput-object v0, p0, Lqml;->e:Ljava/util/List;

    .line 25
    .line 26
    new-instance v0, Lqmm;

    .line 27
    .line 28
    invoke-direct {v0, p1, p2}, Lqmm;-><init>(Landroid/content/Context;Landroid/util/AttributeSet;)V

    .line 29
    .line 30
    .line 31
    new-instance v1, Lqmv;

    .line 32
    .line 33
    invoke-direct {v1, p1, p2}, Lqmv;-><init>(Landroid/content/Context;Landroid/util/AttributeSet;)V

    .line 34
    .line 35
    .line 36
    new-instance p2, Lrfp;

    .line 37
    .line 38
    const/4 v2, 0x0

    .line 39
    invoke-direct {p2, v2}, Lrfp;-><init>([B)V

    .line 40
    .line 41
    .line 42
    iput-object p2, p0, Lqml;->j:Lrfp;

    .line 43
    .line 44
    new-instance v2, Ligp;

    .line 45
    .line 46
    invoke-direct {v2, v0, v1, p2}, Ligp;-><init>(Lqmm;Lqmv;Lrfp;)V

    .line 47
    .line 48
    .line 49
    iput-object v2, p0, Lqml;->k:Ligp;

    .line 50
    .line 51
    invoke-interface {p3, p1, v0}, Lqmk;->apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 52
    .line 53
    .line 54
    move-result-object p1

    .line 55
    check-cast p1, Lqmo;

    .line 56
    .line 57
    iput-object p1, p0, Lqml;->f:Lqmo;

    .line 58
    .line 59
    return-void
.end method

.method private static l(II)I
    .locals 1

    .line 1
    const/4 v0, 0x0

    .line 2
    invoke-static {p0, v0, p1}, Lsag;->e(III)I

    .line 3
    .line 4
    .line 5
    move-result p0

    .line 6
    return p0
.end method

.method private final m(Landroid/view/View;Z)V
    .locals 1

    .line 1
    invoke-virtual {p1, p2}, Landroid/view/View;->setPressed(Z)V

    .line 2
    .line 3
    .line 4
    const v0, 0x7f0b070b

    .line 5
    .line 6
    .line 7
    invoke-virtual {p1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 8
    .line 9
    .line 10
    move-result-object v0

    .line 11
    if-eqz v0, :cond_0

    .line 12
    .line 13
    move-object p1, v0

    .line 14
    :cond_0
    iget-object p0, p0, Lqml;->j:Lrfp;

    .line 15
    .line 16
    if-eqz p2, :cond_1

    .line 17
    .line 18
    new-instance p2, Lpfw;

    .line 19
    .line 20
    const/16 v0, 0x9

    .line 21
    .line 22
    invoke-direct {p2, p0, p1, v0}, Lpfw;-><init>(Ljava/lang/Object;Ljava/lang/Object;I)V

    .line 23
    .line 24
    .line 25
    invoke-virtual {p1, p2}, Landroid/view/View;->post(Ljava/lang/Runnable;)Z

    .line 26
    .line 27
    .line 28
    goto :goto_0

    .line 29
    :cond_1
    invoke-virtual {p0}, Lrfp;->c()Lmix;

    .line 30
    .line 31
    .line 32
    move-result-object p0

    .line 33
    invoke-interface {p0, p1}, Lmix;->m(Landroid/view/View;)V

    .line 34
    .line 35
    .line 36
    :goto_0
    return-void
.end method

.method private static n(FFFFFFI)Z
    .locals 0

    .line 1
    sub-float/2addr p2, p0

    .line 2
    int-to-float p6, p6

    .line 3
    cmpl-float p2, p2, p6

    .line 4
    .line 5
    if-gtz p2, :cond_0

    .line 6
    .line 7
    sub-float/2addr p3, p1

    .line 8
    cmpl-float p2, p3, p6

    .line 9
    .line 10
    if-gtz p2, :cond_0

    .line 11
    .line 12
    sub-float/2addr p0, p4

    .line 13
    cmpl-float p0, p0, p6

    .line 14
    .line 15
    if-gtz p0, :cond_0

    .line 16
    .line 17
    sub-float/2addr p1, p5

    .line 18
    cmpl-float p0, p1, p6

    .line 19
    .line 20
    if-gtz p0, :cond_0

    .line 21
    .line 22
    const/4 p0, 0x0

    .line 23
    goto :goto_0

    .line 24
    :cond_0
    const/4 p0, 0x1

    .line 25
    :goto_0
    return p0
.end method


# virtual methods
.method public a()Lpva;
    .locals 2

    .line 1
    iget v0, p0, Lqml;->g:I

    .line 2
    .line 3
    const/4 v1, -0x1

    .line 4
    if-eq v0, v1, :cond_0

    .line 5
    .line 6
    iget-object p0, p0, Lqml;->e:Ljava/util/List;

    .line 7
    .line 8
    invoke-interface {p0, v0}, Ljava/util/List;->get(I)Ljava/lang/Object;

    .line 9
    .line 10
    .line 11
    move-result-object p0

    .line 12
    check-cast p0, Lpva;

    .line 13
    .line 14
    goto :goto_0

    .line 15
    :cond_0
    const/4 p0, 0x0

    .line 16
    :goto_0
    return-object p0
.end method

.method public b()V
    .locals 3

    .line 1
    iget v0, p0, Lqml;->g:I

    .line 2
    .line 3
    const/4 v1, -0x1

    .line 4
    if-eq v0, v1, :cond_1

    .line 5
    .line 6
    iget-object v0, p0, Lqml;->d:Ljava/util/List;

    .line 7
    .line 8
    invoke-interface {v0}, Ljava/util/List;->isEmpty()Z

    .line 9
    .line 10
    .line 11
    move-result v2

    .line 12
    if-nez v2, :cond_0

    .line 13
    .line 14
    iget v2, p0, Lqml;->g:I

    .line 15
    .line 16
    invoke-interface {v0, v2}, Ljava/util/List;->get(I)Ljava/lang/Object;

    .line 17
    .line 18
    .line 19
    move-result-object v0

    .line 20
    check-cast v0, Landroid/view/View;

    .line 21
    .line 22
    const/4 v2, 0x0

    .line 23
    invoke-direct {p0, v0, v2}, Lqml;->m(Landroid/view/View;Z)V

    .line 24
    .line 25
    .line 26
    :cond_0
    iput v1, p0, Lqml;->g:I

    .line 27
    .line 28
    :cond_1
    return-void
.end method

.method public c(Landroid/widget/LinearLayout;Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;Landroid/view/View;FFLcom/google/android/libraries/inputmethod/metadata/ActionDef;[I)V
    .locals 25

    move-object/from16 v0, p0

    move-object/from16 v1, p1

    move-object/from16 v2, p2

    move-object/from16 v3, p6

    .line 1
    iget-object v4, v0, Lqml;->j:Lrfp;

    iget-object v5, v2, Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;->H:Lwbb;

    iput-object v5, v4, Lrfp;->b:Ljava/lang/Object;

    iget-object v5, v2, Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;->W:Landroid/view/View;

    const/4 v6, 0x1

    const/4 v7, 0x0

    if-eqz v5, :cond_0

    invoke-virtual {v4}, Lrfp;->d()Z

    move-result v4

    if-nez v4, :cond_0

    move v4, v7

    goto :goto_0

    :cond_0
    move v4, v6

    :goto_0
    iput-boolean v4, v0, Lqml;->a:Z

    iput-object v1, v0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 2
    invoke-virtual {v1, v7}, Landroid/widget/LinearLayout;->setLayoutDirection(I)V

    move/from16 v4, p4

    iput v4, v0, Lqml;->i:F

    move/from16 v4, p5

    iput v4, v0, Lqml;->b:F

    iget-object v4, v3, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->f:Lwhe;

    iput-object v4, v0, Lqml;->e:Ljava/util/List;

    iget-object v5, v0, Lqml;->f:Lqmo;

    iget-object v8, v0, Lqml;->k:Ligp;

    iput-object v1, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    iget-object v9, v3, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->e:Lpuc;

    .line 3
    invoke-static {v9}, Lj$/util/Objects;->requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v9

    check-cast v9, Lpuc;

    .line 4
    invoke-virtual {v9}, Lpuc;->ordinal()I

    move-result v9

    const/4 v10, 0x4

    const/4 v11, 0x3

    const/4 v12, 0x2

    if-eq v9, v11, :cond_3

    if-eq v9, v10, :cond_2

    const/4 v13, 0x5

    if-eq v9, v13, :cond_1

    move v9, v12

    goto :goto_1

    :cond_1
    move v9, v11

    goto :goto_1

    :cond_2
    move v9, v10

    goto :goto_1

    :cond_3
    move v9, v6

    :goto_1
    iget-object v13, v5, Lqmo;->d:Lqmn;

    iput v9, v13, Lqmn;->l:I

    const/4 v14, -0x1

    add-int/2addr v9, v14

    if-eq v9, v12, :cond_4

    if-eq v9, v11, :cond_4

    move v9, v6

    goto :goto_2

    :cond_4
    move v9, v7

    .line 5
    :goto_2
    invoke-virtual {v1, v9}, Landroid/widget/LinearLayout;->setOrientation(I)V

    iget-object v9, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    .line 6
    invoke-virtual {v9}, Landroid/widget/LinearLayout;->removeAllViews()V

    .line 7
    invoke-virtual {v4}, Lwhe;->size()I

    move-result v9

    iput v9, v13, Lqmn;->c:I

    iget-object v15, v5, Lqmo;->c:Lqmm;

    move/from16 p4, v14

    .line 8
    iget-boolean v14, v15, Lqmm;->d:Z

    if-eqz v14, :cond_6

    if-ne v9, v10, :cond_6

    iget v9, v15, Lqmm;->c:I

    if-ne v9, v11, :cond_5

    iput v11, v13, Lqmn;->e:I

    iput v12, v13, Lqmn;->d:I

    goto :goto_3

    :cond_5
    move v9, v10

    :cond_6
    add-int/lit8 v9, v9, -0x1

    .line 9
    iget v14, v15, Lqmm;->c:I

    div-int v14, v9, v14

    add-int/2addr v14, v6

    iput v14, v13, Lqmn;->d:I

    .line 10
    div-int/2addr v9, v14

    add-int/2addr v9, v6

    iput v9, v13, Lqmn;->e:I

    .line 11
    :goto_3
    iget-object v9, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    .line 12
    invoke-virtual {v9}, Landroid/widget/LinearLayout;->getOrientation()I

    move-result v9

    if-ne v9, v6, :cond_7

    move v9, v7

    goto :goto_4

    :cond_7
    move v9, v6

    :goto_4
    move v14, v7

    move/from16 p5, v12

    :goto_5
    iget v12, v13, Lqmn;->d:I

    if-ge v14, v12, :cond_c

    iget-object v12, v5, Lqmo;->b:Landroid/content/Context;

    new-instance v7, Landroid/widget/LinearLayout;

    .line 13
    invoke-direct {v7, v12}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V

    .line 14
    invoke-virtual {v7, v9}, Landroid/widget/LinearLayout;->setOrientation(I)V

    iget-object v6, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    .line 15
    invoke-virtual {v6, v7}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V

    const/4 v6, 0x0

    :goto_6
    iget v10, v13, Lqmn;->e:I

    if-ge v6, v10, :cond_b

    iget v11, v13, Lqmn;->d:I

    invoke-virtual {v5, v14, v11}, Lqmo;->f(II)I

    move-result v11

    mul-int/2addr v11, v10

    add-int/2addr v11, v6

    .line 16
    invoke-virtual {v8, v3, v11}, Ligp;->n(Lcom/google/android/libraries/inputmethod/metadata/ActionDef;I)Z

    move-result v10

    if-eqz v10, :cond_8

    iget-object v10, v8, Ligp;->c:Ljava/lang/Object;

    check-cast v10, Lqmm;

    iget v10, v10, Lqmm;->a:I

    .line 17
    invoke-static {v12, v10, v7}, Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;

    goto :goto_7

    .line 18
    :cond_8
    invoke-virtual {v8, v3, v11}, Ligp;->m(Lcom/google/android/libraries/inputmethod/metadata/ActionDef;I)Z

    move-result v10

    if-eqz v10, :cond_9

    iget-object v10, v8, Ligp;->c:Ljava/lang/Object;

    check-cast v10, Lqmm;

    iget v10, v10, Lqmm;->b:I

    .line 19
    invoke-static {v12, v10, v7}, Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;

    goto :goto_7

    :cond_9
    iget-object v10, v8, Ligp;->c:Ljava/lang/Object;

    check-cast v10, Lqmm;

    iget v11, v10, Lqmm;->a:I

    if-nez v11, :cond_a

    .line 20
    iget v11, v10, Lqmm;->b:I

    :cond_a
    invoke-static {v12, v11, v7}, Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;

    .line 21
    :goto_7
    invoke-virtual {v7}, Landroid/widget/LinearLayout;->getChildCount()I

    move-result v10

    add-int/lit8 v10, v10, -0x1

    invoke-virtual {v7, v10}, Landroid/widget/LinearLayout;->getChildAt(I)Landroid/view/View;

    move-result-object v10

    iget-object v11, v8, Ligp;->a:Ljava/lang/Object;

    check-cast v11, Lrfp;

    move/from16 v20, v6

    iget-object v6, v11, Lrfp;->a:Ljava/lang/Object;

    .line 22
    invoke-virtual {v10, v6}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    const/4 v6, 0x0

    .line 23
    invoke-virtual {v10, v6}, Landroid/view/View;->setOnFocusChangeListener(Landroid/view/View$OnFocusChangeListener;)V

    .line 24
    invoke-virtual {v11}, Lrfp;->c()Lmix;

    move-result-object v6

    invoke-interface {v6, v10}, Lmix;->k(Landroid/view/View;)V

    const/4 v6, 0x4

    .line 25
    invoke-virtual {v10, v6}, Landroid/view/View;->setVisibility(I)V

    add-int/lit8 v6, v20, 0x1

    goto :goto_6

    :cond_b
    add-int/lit8 v14, v14, 0x1

    goto :goto_5

    .line 26
    :cond_c
    iget-object v6, v0, Lqml;->d:Ljava/util/List;

    .line 27
    invoke-interface {v6}, Ljava/util/List;->clear()V

    .line 28
    invoke-virtual {v5}, Lqmo;->b()I

    move-result v7

    iput v7, v13, Lqmn;->a:I

    .line 29
    invoke-virtual {v5}, Lqmo;->a()I

    move-result v7

    iput v7, v13, Lqmn;->b:I

    const/4 v7, 0x0

    :goto_8
    iget v9, v13, Lqmn;->d:I

    if-ge v7, v9, :cond_14

    invoke-virtual {v5, v7, v9}, Lqmo;->f(II)I

    move-result v9

    const/4 v10, 0x0

    :goto_9
    iget v11, v13, Lqmn;->e:I

    if-ge v10, v11, :cond_13

    iget-object v11, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    .line 30
    invoke-virtual {v11, v9}, Landroid/widget/LinearLayout;->getChildAt(I)Landroid/view/View;

    move-result-object v11

    check-cast v11, Landroid/view/ViewGroup;

    invoke-virtual {v11, v10}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v11

    iget v12, v13, Lqmn;->e:I

    mul-int/2addr v12, v7

    iget v14, v13, Lqmn;->c:I

    add-int/2addr v12, v10

    if-ge v12, v14, :cond_12

    iget-object v14, v5, Lqmo;->b:Landroid/content/Context;

    .line 31
    invoke-virtual {v4, v12}, Lwhe;->get(I)Ljava/lang/Object;

    move-result-object v20

    move-object/from16 v21, v4

    move-object/from16 v4, v20

    check-cast v4, Lpva;

    .line 32
    invoke-virtual {v8, v3, v12}, Ligp;->n(Lcom/google/android/libraries/inputmethod/metadata/ActionDef;I)Z

    move-result v20

    if-eqz v20, :cond_f

    const v14, 0x7f0b070b

    .line 33
    invoke-virtual {v11, v14}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v14

    check-cast v14, Landroid/widget/TextView;

    move/from16 v20, v7

    .line 34
    invoke-virtual {v3, v12}, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->c(I)Ljava/lang/String;

    move-result-object v7

    .line 35
    invoke-static {v7}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v22

    if-eqz v22, :cond_d

    const-string v22, ""

    move-object/from16 v24, v22

    move/from16 v22, v9

    move-object/from16 v9, v24

    move/from16 v24, v10

    goto :goto_a

    :cond_d
    move/from16 v22, v9

    .line 36
    iget-object v9, v8, Ligp;->b:Ljava/lang/Object;

    move-object/from16 v23, v9

    .line 37
    iget-object v9, v4, Lpva;->d:Lpuz;

    move/from16 v24, v10

    move-object/from16 v10, v23

    check-cast v10, Lqmv;

    invoke-virtual {v10, v7, v9}, Lqmv;->a(Ljava/lang/String;Lpuz;)Ljava/lang/CharSequence;

    move-result-object v9

    .line 38
    :goto_a
    invoke-virtual {v14, v9}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 39
    invoke-static {v7}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v7

    const/4 v9, 0x1

    if-eq v9, v7, :cond_e

    const/4 v7, 0x0

    goto :goto_b

    :cond_e
    const/16 v7, 0x8

    :goto_b
    invoke-virtual {v14, v7}, Landroid/widget/TextView;->setVisibility(I)V

    iget-object v7, v8, Ligp;->a:Ljava/lang/Object;

    .line 40
    iget v9, v4, Lpva;->c:I

    .line 41
    invoke-virtual {v14}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;

    move-result-object v9

    invoke-interface {v9}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object v9

    check-cast v7, Lrfp;

    .line 42
    invoke-virtual {v7}, Lrfp;->c()Lmix;

    move-result-object v7

    const/4 v10, 0x0

    invoke-interface {v7, v9, v10}, Lmix;->c(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;

    move-result-object v7

    .line 43
    invoke-virtual {v11, v7}, Landroid/view/View;->setContentDescription(Ljava/lang/CharSequence;)V

    goto :goto_c

    :cond_f
    move/from16 v20, v7

    move/from16 v22, v9

    move/from16 v24, v10

    .line 44
    invoke-virtual {v8, v3, v12}, Ligp;->m(Lcom/google/android/libraries/inputmethod/metadata/ActionDef;I)Z

    move-result v7

    if-eqz v7, :cond_10

    const v7, 0x7f0b070a

    .line 45
    invoke-virtual {v11, v7}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v7

    check-cast v7, Landroid/widget/ImageView;

    .line 46
    invoke-virtual {v3, v12}, Lcom/google/android/libraries/inputmethod/metadata/ActionDef;->a(I)I

    move-result v9

    invoke-static {v14, v9}, Lsah;->g(Landroid/content/Context;I)I

    move-result v9

    .line 47
    invoke-static {v14}, Llgy;->ck(Landroid/content/Context;)Lmzz;

    move-result-object v14

    invoke-interface {v14, v9}, Lmzz;->g(I)Landroid/graphics/drawable/Drawable;

    move-result-object v14

    invoke-virtual {v7, v14}, Landroid/widget/ImageView;->setImageDrawable(Landroid/graphics/drawable/Drawable;)V

    iget-object v7, v8, Ligp;->a:Ljava/lang/Object;

    .line 48
    iget v14, v4, Lpva;->c:I

    check-cast v7, Lrfp;

    .line 49
    invoke-virtual {v7}, Lrfp;->c()Lmix;

    move-result-object v7

    invoke-interface {v7, v9}, Lmix;->A(I)Ljava/lang/CharSequence;

    move-result-object v7

    .line 50
    invoke-virtual {v11, v7}, Landroid/view/View;->setContentDescription(Ljava/lang/CharSequence;)V

    .line 51
    :cond_10
    :goto_c
    invoke-virtual {v11, v12}, Landroid/view/View;->setId(I)V

    .line 52
    iget v4, v4, Lpva;->c:I

    const/16 v7, -0x27a2

    if-ne v4, v7, :cond_11

    const v4, 0x3ecccccd    # 0.4f

    goto :goto_d

    :cond_11
    const/high16 v4, 0x3f800000    # 1.0f

    .line 53
    :goto_d
    invoke-virtual {v11, v4}, Landroid/view/View;->setAlpha(F)V

    const/4 v4, 0x0

    .line 54
    invoke-virtual {v11, v4}, Landroid/view/View;->setVisibility(I)V

    .line 55
    invoke-interface {v6, v11}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 56
    invoke-virtual {v11, v4, v4}, Landroid/view/View;->measure(II)V

    iget v4, v13, Lqmn;->a:I

    .line 57
    invoke-virtual {v11}, Landroid/view/View;->getMeasuredWidth()I

    move-result v7

    invoke-static {v4, v7}, Ljava/lang/Math;->max(II)I

    move-result v4

    iput v4, v13, Lqmn;->a:I

    goto :goto_e

    :cond_12
    move-object/from16 v21, v4

    move/from16 v20, v7

    move/from16 v22, v9

    move/from16 v24, v10

    move/from16 v4, p4

    .line 58
    invoke-virtual {v11, v4}, Landroid/view/View;->setId(I)V

    :goto_e
    add-int/lit8 v4, v24, 0x1

    move v10, v4

    move/from16 v7, v20

    move-object/from16 v4, v21

    move/from16 v9, v22

    const/16 p4, -0x1

    goto/16 :goto_9

    :cond_13
    move/from16 v20, v7

    add-int/lit8 v7, v20, 0x1

    const/16 p4, -0x1

    goto/16 :goto_8

    :cond_14
    iget v3, v13, Lqmn;->a:I

    iget v4, v13, Lqmn;->b:I

    const/4 v7, 0x0

    .line 59
    :goto_f
    invoke-virtual {v1}, Landroid/widget/LinearLayout;->getChildCount()I

    move-result v8

    if-ge v7, v8, :cond_18

    .line 60
    invoke-virtual {v1, v7}, Landroid/widget/LinearLayout;->getChildAt(I)Landroid/view/View;

    move-result-object v8

    check-cast v8, Landroid/view/ViewGroup;

    const/4 v9, 0x0

    .line 61
    :goto_10
    invoke-virtual {v8}, Landroid/view/ViewGroup;->getChildCount()I

    move-result v10

    if-ge v9, v10, :cond_17

    .line 62
    invoke-virtual {v8, v9}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v10

    invoke-virtual {v10}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v10

    if-lez v3, :cond_15

    .line 63
    iput v3, v10, Landroid/view/ViewGroup$LayoutParams;->width:I

    :cond_15
    if-lez v4, :cond_16

    .line 64
    iput v4, v10, Landroid/view/ViewGroup$LayoutParams;->height:I

    :cond_16
    add-int/lit8 v9, v9, 0x1

    goto :goto_10

    :cond_17
    add-int/lit8 v7, v7, 0x1

    goto :goto_f

    :cond_18
    iget-object v1, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    const/4 v4, 0x0

    .line 65
    invoke-virtual {v1, v4, v4}, Landroid/widget/LinearLayout;->measure(II)V

    new-instance v1, Landroid/graphics/Rect;

    .line 66
    invoke-direct {v1}, Landroid/graphics/Rect;-><init>()V

    move-object/from16 v3, p3

    .line 67
    invoke-static {v3, v2, v1}, Ltgb;->H(Landroid/view/View;Landroid/view/View;Landroid/graphics/Rect;)V

    .line 68
    invoke-virtual {v15}, Lqmm;->a()Z

    move-result v3

    if-eqz v3, :cond_19

    iput v4, v13, Lqmn;->f:I

    goto :goto_11

    .line 69
    :cond_19
    iget v3, v13, Lqmn;->e:I

    const/4 v4, -0x1

    add-int/2addr v3, v4

    div-int/lit8 v3, v3, 0x2

    iput v3, v13, Lqmn;->f:I

    .line 70
    :goto_11
    invoke-virtual {v2}, Landroid/view/View;->getPaddingLeft()I

    move-result v3

    .line 71
    invoke-virtual {v2}, Landroid/view/View;->getWidth()I

    move-result v4

    invoke-virtual {v2}, Landroid/view/View;->getPaddingRight()I

    move-result v7

    sub-int/2addr v4, v7

    invoke-virtual {v13}, Lqmn;->b()Z

    move-result v7

    if-eqz v7, :cond_1f

    iget-object v2, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    .line 72
    invoke-virtual {v2}, Landroid/widget/LinearLayout;->getMeasuredWidth()I

    move-result v2

    iget-object v7, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    .line 73
    invoke-virtual {v7}, Landroid/widget/LinearLayout;->getMeasuredHeight()I

    move-result v7

    .line 74
    invoke-virtual {v1}, Landroid/graphics/Rect;->centerX()I

    move-result v8

    .line 75
    iget v9, v1, Landroid/graphics/Rect;->top:I

    sub-int/2addr v9, v7

    iput v9, v13, Lqmn;->h:I

    .line 76
    invoke-virtual {v15}, Lqmm;->a()Z

    move-result v7

    if-eqz v7, :cond_1a

    int-to-double v9, v4

    int-to-double v11, v2

    .line 77
    invoke-virtual {v1}, Landroid/graphics/Rect;->centerX()I

    move-result v7

    move-wide/from16 v18, v9

    int-to-double v9, v7

    invoke-virtual {v5}, Lqmo;->c()D

    move-result-wide v20

    sub-double v9, v9, v20

    add-double/2addr v9, v11

    cmpl-double v7, v9, v18

    if-lez v7, :cond_1a

    iget v7, v13, Lqmn;->e:I

    const/4 v9, -0x1

    add-int/2addr v7, v9

    iput v7, v13, Lqmn;->f:I

    .line 78
    :cond_1a
    invoke-virtual {v5}, Lqmo;->c()D

    move-result-wide v9

    int-to-double v11, v8

    sub-double v18, v11, v9

    iget v7, v13, Lqmn;->j:I

    add-int/2addr v7, v3

    move/from16 p1, v8

    int-to-double v7, v7

    cmpg-double v7, v18, v7

    const-wide/high16 v20, -0x4020000000000000L    # -0.5

    if-gez v7, :cond_1b

    sub-int v8, p1, v3

    iget-object v7, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    .line 79
    invoke-virtual {v7}, Landroid/widget/LinearLayout;->getPaddingLeft()I

    move-result v7

    sub-int/2addr v8, v7

    iget v7, v13, Lqmn;->a:I

    int-to-float v7, v7

    int-to-float v8, v8

    div-float/2addr v8, v7

    float-to-double v7, v8

    add-double v7, v7, v20

    double-to-int v7, v7

    const/4 v8, 0x0

    .line 80
    invoke-static {v8, v7}, Ljava/lang/Math;->max(II)I

    move-result v7

    iget v8, v13, Lqmn;->e:I

    const/4 v9, -0x1

    add-int/2addr v8, v9

    .line 81
    invoke-static {v8, v7}, Ljava/lang/Math;->min(II)I

    move-result v7

    iput v7, v13, Lqmn;->f:I

    .line 82
    invoke-virtual {v5}, Lqmo;->c()D

    move-result-wide v9

    goto :goto_12

    :cond_1b
    int-to-double v7, v4

    move-wide/from16 v22, v7

    int-to-double v7, v2

    .line 83
    iget v14, v13, Lqmn;->k:I

    move-wide/from16 p2, v7

    int-to-double v7, v14

    add-double v18, v18, p2

    add-double v18, v18, v7

    cmpl-double v7, v18, v22

    if-lez v7, :cond_1c

    sub-int v7, v4, p1

    iget-object v8, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    .line 84
    invoke-virtual {v8}, Landroid/widget/LinearLayout;->getPaddingRight()I

    move-result v8

    sub-int/2addr v7, v8

    iget v8, v13, Lqmn;->a:I

    int-to-float v8, v8

    int-to-float v7, v7

    div-float/2addr v7, v8

    float-to-double v7, v7

    add-double v7, v7, v20

    double-to-int v7, v7

    const/4 v8, 0x0

    .line 85
    invoke-static {v8, v7}, Ljava/lang/Math;->max(II)I

    move-result v7

    iget v9, v13, Lqmn;->e:I

    const/4 v10, -0x1

    add-int/2addr v9, v10

    sub-int/2addr v9, v7

    .line 86
    invoke-static {v8, v9}, Ljava/lang/Math;->max(II)I

    move-result v7

    iput v7, v13, Lqmn;->f:I

    .line 87
    invoke-virtual {v5}, Lqmo;->c()D

    move-result-wide v9

    :cond_1c
    :goto_12
    sub-double/2addr v11, v9

    double-to-int v7, v11

    if-lt v7, v3, :cond_1e

    add-int v3, v7, v2

    if-le v3, v4, :cond_1d

    sub-int v3, v4, v2

    goto :goto_13

    :cond_1d
    move v3, v7

    .line 88
    :cond_1e
    :goto_13
    iput v3, v13, Lqmn;->g:I

    goto :goto_14

    .line 89
    :cond_1f
    iget v7, v0, Lqml;->b:F

    iget-object v8, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    .line 90
    invoke-virtual {v8}, Landroid/widget/LinearLayout;->getMeasuredWidth()I

    move-result v8

    iget-object v9, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    .line 91
    invoke-virtual {v9}, Landroid/widget/LinearLayout;->getMeasuredHeight()I

    move-result v9

    .line 92
    invoke-virtual {v2}, Landroid/view/View;->getHeight()I

    move-result v2

    .line 93
    invoke-static {v7}, Ljava/lang/Math;->round(F)I

    move-result v7

    .line 94
    invoke-virtual {v5}, Lqmo;->d()D

    move-result-wide v10

    move/from16 p1, v8

    int-to-double v7, v7

    sub-double v19, v7, v10

    move-wide/from16 v21, v7

    int-to-double v7, v9

    move-wide/from16 v23, v7

    int-to-double v7, v2

    add-double v19, v19, v23

    cmpl-double v2, v19, v7

    if-lez v2, :cond_20

    iget v2, v13, Lqmn;->e:I

    const/4 v9, -0x1

    add-int/2addr v2, v9

    iput v2, v13, Lqmn;->f:I

    .line 95
    invoke-virtual {v5}, Lqmo;->d()D

    move-result-wide v10

    :cond_20
    sub-double v7, v21, v10

    double-to-int v2, v7

    iput v2, v13, Lqmn;->h:I

    invoke-virtual {v13}, Lqmn;->c()Z

    move-result v2

    if-eqz v2, :cond_21

    .line 96
    iget v3, v1, Landroid/graphics/Rect;->right:I

    iput v3, v13, Lqmn;->g:I

    add-int v8, v3, p1

    if-le v8, v4, :cond_23

    .line 97
    iget v2, v1, Landroid/graphics/Rect;->left:I

    sub-int v3, v2, p1

    iput v3, v13, Lqmn;->g:I

    const/4 v2, 0x4

    iput v2, v13, Lqmn;->l:I

    goto :goto_14

    .line 98
    :cond_21
    iget v2, v1, Landroid/graphics/Rect;->left:I

    sub-int v2, v2, p1

    iput v2, v13, Lqmn;->g:I

    if-ge v2, v3, :cond_22

    .line 99
    iget v3, v1, Landroid/graphics/Rect;->right:I

    iput v3, v13, Lqmn;->g:I

    const/4 v2, 0x3

    iput v2, v13, Lqmn;->l:I

    goto :goto_14

    :cond_22
    move v3, v2

    .line 100
    :cond_23
    :goto_14
    iget v2, v1, Landroid/graphics/Rect;->left:I

    sub-int/2addr v3, v2

    const/16 v16, 0x0

    aput v3, p7, v16

    iget v2, v13, Lqmn;->h:I

    .line 101
    iget v1, v1, Landroid/graphics/Rect;->top:I

    sub-int/2addr v2, v1

    const/16 v17, 0x1

    aput v2, p7, v17

    .line 102
    iget-boolean v1, v15, Lqmm;->g:Z

    if-eqz v1, :cond_29

    iget v1, v13, Lqmn;->f:I

    if-eqz v1, :cond_29

    .line 103
    iget-object v1, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    .line 104
    invoke-virtual {v1}, Landroid/widget/LinearLayout;->getChildCount()I

    move-result v1

    const/4 v4, 0x0

    :goto_15
    if-ge v4, v1, :cond_29

    iget-object v2, v5, Lqmo;->e:Landroid/widget/LinearLayout;

    invoke-virtual {v5, v4, v1}, Lqmo;->f(II)I

    move-result v3

    .line 105
    invoke-virtual {v2, v3}, Landroid/widget/LinearLayout;->getChildAt(I)Landroid/view/View;

    move-result-object v2

    check-cast v2, Landroid/view/ViewGroup;

    iget v3, v13, Lqmn;->e:I

    .line 106
    new-array v7, v3, [Landroid/view/View;

    const/4 v8, 0x0

    :goto_16
    if-ge v8, v3, :cond_27

    iget v9, v13, Lqmn;->f:I

    iget v10, v13, Lqmn;->e:I

    const/4 v11, -0x1

    add-int/2addr v10, v11

    sub-int/2addr v10, v9

    .line 107
    invoke-static {v9, v10}, Ljava/lang/Math;->min(II)I

    move-result v9

    const/16 v17, 0x1

    shl-int/lit8 v9, v9, 0x1

    add-int/lit8 v9, v9, 0x1

    iget v10, v13, Lqmn;->f:I

    iget v12, v13, Lqmn;->e:I

    div-int/lit8 v14, v12, 0x2

    if-ge v8, v9, :cond_25

    add-int/lit8 v9, v8, 0x1

    div-int/lit8 v9, v9, 0x2

    rem-int/lit8 v12, v8, 0x2

    if-eqz v12, :cond_24

    move/from16 v12, v17

    goto :goto_17

    :cond_24
    move v12, v11

    :goto_17
    mul-int/2addr v9, v12

    add-int/2addr v10, v9

    goto :goto_18

    :cond_25
    if-ge v10, v14, :cond_26

    move v10, v8

    goto :goto_18

    :cond_26
    add-int/lit8 v12, v12, -0x1

    sub-int v10, v12, v8

    .line 108
    :goto_18
    invoke-virtual {v2, v8}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v9

    aput-object v9, v7, v10

    add-int/lit8 v8, v8, 0x1

    goto :goto_16

    .line 109
    :cond_27
    invoke-virtual {v2}, Landroid/view/ViewGroup;->removeAllViews()V

    const/4 v8, 0x0

    :goto_19
    if-ge v8, v3, :cond_28

    .line 110
    aget-object v9, v7, v8

    invoke-virtual {v2, v9}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    add-int/lit8 v8, v8, 0x1

    goto :goto_19

    :cond_28
    add-int/lit8 v4, v4, 0x1

    goto :goto_15

    .line 111
    :cond_29
    iget v1, v13, Lqmn;->c:I

    if-eqz v1, :cond_2a

    const/4 v8, 0x0

    .line 112
    invoke-interface {v6, v8}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/view/View;

    invoke-virtual {v1}, Landroid/view/View;->getMeasuredHeight()I

    move-result v1

    iput v1, v13, Lqmn;->b:I

    :cond_2a
    iget v1, v13, Lqmn;->a:I

    int-to-float v1, v1

    .line 113
    iget v2, v15, Lqmm;->e:F

    mul-float/2addr v1, v2

    float-to-int v1, v1

    iput v1, v13, Lqmn;->i:I

    .line 114
    invoke-virtual {v0}, Lqml;->e()V

    return-void
.end method

.method public d()V
    .locals 1

    .line 1
    const/4 v0, -0x1

    .line 2
    iput v0, p0, Lqml;->g:I

    .line 3
    .line 4
    iput v0, p0, Lqml;->h:I

    .line 5
    .line 6
    const/4 v0, 0x1

    .line 7
    iput-boolean v0, p0, Lqml;->a:Z

    .line 8
    .line 9
    return-void
.end method

.method public e()V
    .locals 5

    .line 1
    const/4 v4, 0x0

    .line 2
    iget-object v0, p0, Lqml;->e:Ljava/util/List;

    .line 3
    .line 4
    if-eqz v0, :cond_0

    .line 5
    .line 6
    invoke-interface {v0}, Ljava/util/List;->size()I

    .line 7
    .line 8
    .line 9
    move-result v1

    .line 10
    const/16 v2, 0x9

    .line 11
    .line 12
    if-ne v1, v2, :cond_0

    .line 13
    .line 14
    const/4 v1, 0x3

    .line 15
    invoke-interface {v0, v1}, Ljava/util/List;->get(I)Ljava/lang/Object;

    .line 16
    .line 17
    .line 18
    move-result-object v1

    .line 19
    instance-of v2, v1, Lpva;

    .line 20
    .line 21
    if-eqz v2, :cond_0

    .line 22
    .line 23
    iget-object v2, v1, Lpva;->e:Ljava/lang/Object;

    .line 24
    .line 25
    instance-of v3, v2, Ljava/lang/CharSequence;

    .line 26
    .line 27
    if-eqz v3, :cond_0

    .line 28
    .line 29
    invoke-interface {v2}, Ljava/lang/CharSequence;->length()I

    .line 30
    .line 31
    .line 32
    move-result v3

    .line 33
    if-eqz v3, :cond_0

    .line 34
    .line 35
    const/4 v3, 0x0

    .line 36
    invoke-static {v2, v3}, Ljava/lang/Character;->codePointAt(Ljava/lang/CharSequence;I)I

    .line 37
    .line 38
    .line 39
    move-result v3

    .line 40
    const/high16 v2, 0x10000

    .line 41
    .line 42
    if-lt v3, v2, :cond_0

    .line 43
    .line 44
    const/4 v4, 0x3

    .line 45
    iget-object v0, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 46
    .line 47
    invoke-virtual {v0}, Landroid/view/View;->getParent()Landroid/view/ViewParent;

    .line 48
    .line 49
    .line 50
    move-result-object v0

    .line 51
    check-cast v0, Landroid/view/View;

    .line 52
    .line 53
    const v1, 0x7f080786

    .line 54
    .line 55
    .line 56
    invoke-virtual {v0, v1}, Landroid/view/View;->setBackgroundResource(I)V

    .line 57
    .line 58
    .line 59
    goto :goto_0

    .line 60
    :cond_0
    iget-object v0, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 61
    .line 62
    invoke-virtual {v0}, Landroid/view/View;->getParent()Landroid/view/ViewParent;

    .line 63
    .line 64
    .line 65
    move-result-object v0

    .line 66
    check-cast v0, Landroid/view/View;

    .line 67
    const v1, 0x7f080319

    invoke-virtual {v0, v1}, Landroid/view/View;->setBackgroundResource(I)V

    nop

    :goto_0
    iget-object v0, p0, Lqml;->d:Ljava/util/List;

    invoke-interface {v0}, Ljava/util/List;->isEmpty()Z

    move-result v1

    if-nez v1, :cond_2

    iget-object v1, p0, Lqml;->f:Lqmo;

    iget-object v2, v1, Lqmo;->c:Lqmm;

    iget-boolean v3, v2, Lqmm;->d:Z

    nop

    if-nez v3, :cond_1

    iget-boolean v2, v2, Lqmm;->g:Z

    if-nez v2, :cond_1

    iget-object v1, v1, Lqmo;->d:Lqmn;

    iget v4, v1, Lqmn;->f:I

    :cond_1
    invoke-interface {v0, v4}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/view/View;

    iget-boolean v1, p0, Lqml;->a:Z

    invoke-direct {p0, v0, v1}, Lqml;->m(Landroid/view/View;Z)V

    iput v4, p0, Lqml;->g:I

    iput v4, p0, Lqml;->h:I

    :cond_2
    return-void
.end method

.method public final f(II)V
    .locals 0

    .line 1
    iget-object p0, p0, Lqml;->f:Lqmo;

    .line 2
    .line 3
    iget-object p0, p0, Lqmo;->d:Lqmn;

    .line 4
    .line 5
    iput p1, p0, Lqmn;->j:I

    .line 6
    .line 7
    iput p2, p0, Lqmn;->k:I

    .line 8
    .line 9
    return-void
.end method

.method public final g(Landroid/view/View$OnClickListener;)V
    .locals 0

    .line 1
    iget-object p0, p0, Lqml;->j:Lrfp;

    .line 2
    .line 3
    iput-object p1, p0, Lrfp;->a:Ljava/lang/Object;

    .line 4
    .line 5
    return-void
.end method

.method public final h(Landroid/view/View;)V
    .locals 4

    .line 1
    invoke-virtual {p1}, Landroid/view/View;->getVisibility()I

    .line 2
    .line 3
    .line 4
    move-result v0

    .line 5
    if-nez v0, :cond_1

    .line 6
    .line 7
    invoke-virtual {p1}, Landroid/view/View;->getId()I

    .line 8
    .line 9
    .line 10
    move-result v0

    .line 11
    if-ltz v0, :cond_1

    .line 12
    .line 13
    iget-object v1, p0, Lqml;->d:Ljava/util/List;

    .line 14
    .line 15
    invoke-interface {v1}, Ljava/util/List;->size()I

    .line 16
    .line 17
    .line 18
    move-result v2

    .line 19
    if-ge v0, v2, :cond_1

    .line 20
    .line 21
    iget v2, p0, Lqml;->g:I

    .line 22
    .line 23
    if-eq v2, v0, :cond_1

    .line 24
    .line 25
    const/4 v3, -0x1

    .line 26
    if-eq v2, v3, :cond_0

    .line 27
    .line 28
    invoke-interface {v1, v2}, Ljava/util/List;->get(I)Ljava/lang/Object;

    .line 29
    .line 30
    .line 31
    move-result-object v1

    .line 32
    check-cast v1, Landroid/view/View;

    .line 33
    .line 34
    if-eqz v1, :cond_0

    .line 35
    .line 36
    const/4 v2, 0x0

    .line 37
    invoke-direct {p0, v1, v2}, Lqml;->m(Landroid/view/View;Z)V

    .line 38
    .line 39
    .line 40
    :cond_0
    iput v0, p0, Lqml;->g:I

    .line 41
    .line 42
    iget-boolean v0, p0, Lqml;->a:Z

    .line 43
    .line 44
    if-eqz v0, :cond_1

    .line 45
    .line 46
    const/4 v0, 0x1

    .line 47
    invoke-direct {p0, p1, v0}, Lqml;->m(Landroid/view/View;Z)V

    .line 48
    .line 49
    .line 50
    :cond_1
    return-void
.end method

.method protected i(II)V
    .locals 3

    .line 1
    iget-object v0, p0, Lqml;->f:Lqmo;

    .line 2
    .line 3
    iget-object v0, v0, Lqmo;->d:Lqmn;

    .line 4
    .line 5
    iget v1, v0, Lqmn;->b:I

    .line 6
    .line 7
    div-int/2addr p2, v1

    .line 8
    iget v1, v0, Lqmn;->a:I

    .line 9
    .line 10
    div-int/2addr p1, v1

    .line 11
    invoke-virtual {v0}, Lqmn;->b()Z

    .line 12
    .line 13
    .line 14
    move-result v0

    .line 15
    iget-object v1, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 16
    .line 17
    const/4 v2, 0x0

    .line 18
    if-eqz v0, :cond_0

    .line 19
    .line 20
    invoke-virtual {v1}, Landroid/widget/LinearLayout;->getChildCount()I

    .line 21
    .line 22
    .line 23
    move-result v0

    .line 24
    add-int/lit8 v0, v0, -0x1

    .line 25
    .line 26
    invoke-static {p2, v0}, Lqml;->l(II)I

    .line 27
    .line 28
    .line 29
    move-result p2

    .line 30
    invoke-virtual {v1, p2}, Landroid/widget/LinearLayout;->getChildAt(I)Landroid/view/View;

    .line 31
    .line 32
    .line 33
    move-result-object p2

    .line 34
    check-cast p2, Landroid/view/ViewGroup;

    .line 35
    .line 36
    if-eqz p2, :cond_1

    .line 37
    .line 38
    invoke-virtual {p2}, Landroid/view/ViewGroup;->getChildCount()I

    .line 39
    .line 40
    .line 41
    move-result v0

    .line 42
    add-int/lit8 v0, v0, -0x1

    .line 43
    .line 44
    invoke-static {p1, v0}, Lqml;->l(II)I

    .line 45
    .line 46
    .line 47
    move-result p1

    .line 48
    invoke-virtual {p2, p1}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    .line 49
    .line 50
    .line 51
    move-result-object v2

    .line 52
    goto :goto_0

    .line 53
    :cond_0
    invoke-virtual {v1}, Landroid/widget/LinearLayout;->getChildCount()I

    .line 54
    .line 55
    .line 56
    move-result v0

    .line 57
    add-int/lit8 v0, v0, -0x1

    .line 58
    .line 59
    invoke-static {p1, v0}, Lqml;->l(II)I

    .line 60
    .line 61
    .line 62
    move-result p1

    .line 63
    invoke-virtual {v1, p1}, Landroid/widget/LinearLayout;->getChildAt(I)Landroid/view/View;

    .line 64
    .line 65
    .line 66
    move-result-object p1

    .line 67
    check-cast p1, Landroid/view/ViewGroup;

    .line 68
    .line 69
    if-eqz p1, :cond_1

    .line 70
    .line 71
    invoke-virtual {p1}, Landroid/view/ViewGroup;->getChildCount()I

    .line 72
    .line 73
    .line 74
    move-result v0

    .line 75
    add-int/lit8 v0, v0, -0x1

    .line 76
    .line 77
    invoke-static {p2, v0}, Lqml;->l(II)I

    .line 78
    .line 79
    .line 80
    move-result p2

    .line 81
    invoke-virtual {p1, p2}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    .line 82
    .line 83
    .line 84
    move-result-object v2

    .line 85
    :cond_1
    :goto_0
    if-eqz v2, :cond_2

    .line 86
    .line 87
    invoke-virtual {p0, v2}, Lqml;->h(Landroid/view/View;)V

    .line 88
    .line 89
    .line 90
    :cond_2
    return-void
.end method

.method public final j()Z
    .locals 2

    .line 1
    iget v0, p0, Lqml;->g:I

    .line 2
    .line 3
    const/4 v1, -0x1

    .line 4
    if-ne v0, v1, :cond_0

    .line 5
    .line 6
    iget-object p0, p0, Lqml;->j:Lrfp;

    .line 7
    .line 8
    invoke-virtual {p0}, Lrfp;->d()Z

    .line 9
    .line 10
    .line 11
    move-result p0

    .line 12
    if-nez p0, :cond_0

    .line 13
    .line 14
    const/4 p0, 0x1

    .line 15
    goto :goto_0

    .line 16
    :cond_0
    const/4 p0, 0x0

    .line 17
    :goto_0
    return p0
.end method

.method public final k(FF)Lpva;
    .locals 10

    .line 1
    iget-object v0, p0, Lqml;->e:Ljava/util/List;

    .line 2
    .line 3
    if-eqz v0, :cond_5

    .line 4
    .line 5
    invoke-interface {v0}, Ljava/util/List;->size()I

    .line 6
    .line 7
    .line 8
    move-result v1

    .line 9
    const/16 v2, 0x9

    .line 10
    .line 11
    if-ne v1, v2, :cond_5

    .line 12
    .line 13
    const/4 v1, 0x3

    .line 14
    invoke-interface {v0, v1}, Ljava/util/List;->get(I)Ljava/lang/Object;

    .line 15
    .line 16
    .line 17
    move-result-object v1

    .line 18
    instance-of v2, v1, Lpva;

    .line 19
    .line 20
    if-eqz v2, :cond_5

    .line 21
    .line 22
    iget-object v2, v1, Lpva;->e:Ljava/lang/Object;

    .line 23
    .line 24
    instance-of v3, v2, Ljava/lang/CharSequence;

    .line 25
    .line 26
    if-eqz v3, :cond_5

    .line 27
    .line 28
    invoke-interface {v2}, Ljava/lang/CharSequence;->length()I

    .line 29
    .line 30
    .line 31
    move-result v3

    .line 32
    if-eqz v3, :cond_5

    .line 33
    .line 34
    const/4 v3, 0x0

    .line 35
    invoke-static {v2, v3}, Ljava/lang/Character;->codePointAt(Ljava/lang/CharSequence;I)I

    .line 36
    .line 37
    .line 38
    move-result v3

    .line 39
    const/high16 v2, 0x10000

    .line 40
    .line 41
    if-lt v3, v2, :cond_5

    .line 42
    .line 43
    iget-object v0, p0, Lqml;->d:Ljava/util/List;

    .line 44
    .line 45
    invoke-interface {v0}, Ljava/util/List;->isEmpty()Z

    .line 46
    .line 47
    .line 48
    move-result v0

    .line 49
    if-nez v0, :cond_5

    .line 50
    .line 51
    instance-of v0, p0, Lqmu;

    .line 52
    .line 53
    if-eqz v0, :cond_5

    .line 54
    .line 55
    check-cast p0, Lqmu;

    .line 56
    .line 57
    iget v1, p0, Lqml;->i:F

    .line 58
    .line 59
    float-to-int v1, v1

    .line 60
    iget v3, p0, Lqml;->b:F

    .line 61
    .line 62
    float-to-int v3, v3

    .line 63
    if-nez v1, :cond_0

    .line 64
    .line 65
    if-eqz v3, :cond_5

    .line 66
    .line 67
    :cond_0
    iget-object v9, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 68
    .line 69
    invoke-virtual {v9}, Landroid/widget/LinearLayout;->getWidth()I

    .line 70
    .line 71
    .line 72
    move-result v4

    .line 73
    invoke-virtual {v9}, Landroid/view/View;->getPaddingLeft()I

    .line 74
    .line 75
    .line 76
    move-result v2

    .line 77
    sub-int v4, v4, v2

    .line 78
    .line 79
    invoke-virtual {v9}, Landroid/view/View;->getPaddingRight()I

    .line 80
    .line 81
    .line 82
    move-result v2

    .line 83
    sub-int v4, v4, v2

    .line 84
    .line 85
    invoke-virtual {v9}, Landroid/widget/LinearLayout;->getHeight()I

    .line 86
    .line 87
    .line 88
    move-result v5

    .line 89
    invoke-virtual {v9}, Landroid/view/View;->getPaddingTop()I

    .line 90
    .line 91
    .line 92
    move-result v2

    .line 93
    sub-int v5, v5, v2

    .line 94
    .line 95
    invoke-virtual {v9}, Landroid/view/View;->getPaddingBottom()I

    .line 96
    .line 97
    .line 98
    move-result v2

    .line 99
    sub-int v5, v5, v2

    .line 100
    .line 101
    const/4 v2, 0x3

    .line 102
    div-int v6, v4, v2

    .line 103
    .line 104
    div-int v7, v5, v2

    .line 105
    .line 106
    const/4 v2, 0x2

    .line 107
    div-int v8, v4, v2

    .line 108
    .line 109
    div-int v9, v5, v2

    .line 110
    .line 111
    float-to-int v0, p1

    .line 112
    sub-int v0, v0, v1

    .line 113
    .line 114
    float-to-int v2, p2

    .line 115
    sub-int v2, v2, v3

    .line 116
    .line 117
    neg-int v1, v0

    .line 118
    neg-int v3, v2

    .line 119
    if-ge v0, v6, :cond_1

    .line 120
    .line 121
    if-ge v1, v6, :cond_1

    .line 122
    .line 123
    if-ge v2, v7, :cond_1

    .line 124
    .line 125
    if-ge v3, v7, :cond_1

    .line 126
    .line 127
    move v6, v8

    .line 128
    move v7, v9

    .line 129
    goto :goto_0

    .line 130
    :cond_1
    if-gt v2, v0, :cond_2

    .line 131
    .line 132
    if-gt v2, v1, :cond_2

    .line 133
    .line 134
    move v6, v8

    .line 135
    sub-int v7, v9, v7

    .line 136
    .line 137
    goto :goto_0

    .line 138
    :cond_2
    if-lt v2, v0, :cond_3

    .line 139
    .line 140
    if-lt v2, v1, :cond_3

    .line 141
    .line 142
    move v6, v8

    .line 143
    add-int v7, v9, v7

    .line 144
    .line 145
    goto :goto_0

    .line 146
    :cond_3
    if-gt v0, v2, :cond_4

    .line 147
    .line 148
    if-gt v0, v3, :cond_4

    .line 149
    .line 150
    sub-int v6, v8, v6

    .line 151
    .line 152
    move v7, v9

    .line 153
    goto :goto_0

    .line 154
    :cond_4
    add-int v6, v8, v6

    .line 155
    .line 156
    move v7, v9

    .line 157
    :goto_0
    invoke-virtual {p0, v6, v7}, Lqml;->i(II)V

    .line 158
    .line 159
    .line 160
    invoke-virtual {p0}, Lqmu;->n()V

    .line 161
    .line 162
    .line 163
    invoke-virtual {p0}, Lqml;->a()Lpva;

    .line 164
    .line 165
    .line 166
    move-result-object v0

    .line 167
    return-object v0

    .line 168
    nop

    .line 169
    :cond_5
    iget-object v0, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 170
    .line 171
    const/4 v1, 0x0

    .line 172
    if-eqz v0, :cond_11

    .line 173
    .line 174
    iget-object v0, p0, Lqml;->d:Ljava/util/List;

    .line 175
    .line 176
    invoke-interface {v0}, Ljava/util/List;->isEmpty()Z

    .line 177
    .line 178
    .line 179
    move-result v2

    .line 180
    if-nez v2, :cond_11

    .line 181
    .line 182
    const/4 v2, 0x0

    .line 183
    invoke-interface {v0, v2}, Ljava/util/List;->get(I)Ljava/lang/Object;

    .line 184
    .line 185
    .line 186
    move-result-object v0

    .line 187
    check-cast v0, Landroid/view/View;

    .line 188
    .line 189
    invoke-virtual {v0}, Landroid/view/View;->getWidth()I

    .line 190
    .line 191
    .line 192
    move-result v2

    .line 193
    if-eqz v2, :cond_11

    .line 194
    .line 195
    invoke-virtual {v0}, Landroid/view/View;->getHeight()I

    .line 196
    .line 197
    .line 198
    move-result v0

    .line 199
    if-eqz v0, :cond_11

    .line 200
    .line 201
    iget-object v0, p0, Lqml;->f:Lqmo;

    .line 202
    .line 203
    iget-object v2, v0, Lqmo;->d:Lqmn;

    .line 204
    .line 205
    iget v3, v2, Lqmn;->l:I

    .line 206
    .line 207
    add-int/lit8 v4, v3, -0x1

    .line 208
    .line 209
    if-eqz v3, :cond_10

    .line 210
    .line 211
    if-eqz v4, :cond_9

    .line 212
    .line 213
    const/4 v3, 0x1

    .line 214
    if-eq v4, v3, :cond_8

    .line 215
    .line 216
    const/4 v3, 0x2

    .line 217
    if-eq v4, v3, :cond_7

    .line 218
    .line 219
    const/4 v3, 0x3

    .line 220
    if-ne v4, v3, :cond_6

    .line 221
    .line 222
    iget v1, v2, Lqmn;->g:I

    .line 223
    .line 224
    int-to-float v5, v1

    .line 225
    iget v1, v2, Lqmn;->h:I

    .line 226
    .line 227
    int-to-float v6, v1

    .line 228
    iget v7, p0, Lqml;->i:F

    .line 229
    .line 230
    iget-object v3, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 231
    .line 232
    invoke-virtual {v3}, Landroid/widget/LinearLayout;->getHeight()I

    .line 233
    .line 234
    .line 235
    move-result v3

    .line 236
    add-int/2addr v1, v3

    .line 237
    iget v9, v2, Lqmn;->i:I

    .line 238
    .line 239
    int-to-float v8, v1

    .line 240
    move v3, p1

    .line 241
    move v4, p2

    .line 242
    invoke-static/range {v3 .. v9}, Lqml;->n(FFFFFFI)Z

    .line 243
    .line 244
    .line 245
    move-result p1

    .line 246
    goto :goto_1

    .line 247
    :cond_6
    new-instance p0, Ljava/lang/RuntimeException;

    .line 248
    .line 249
    invoke-direct {p0, v1, v1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;Ljava/lang/Throwable;)V

    .line 250
    .line 251
    .line 252
    throw p0

    .line 253
    :cond_7
    move v3, p1

    .line 254
    move v4, p2

    .line 255
    iget v5, p0, Lqml;->i:F

    .line 256
    .line 257
    iget p1, v2, Lqmn;->h:I

    .line 258
    .line 259
    int-to-float v6, p1

    .line 260
    iget p1, v2, Lqmn;->g:I

    .line 261
    .line 262
    iget-object p2, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 263
    .line 264
    invoke-virtual {p2}, Landroid/widget/LinearLayout;->getWidth()I

    .line 265
    .line 266
    .line 267
    move-result p2

    .line 268
    add-int/2addr p1, p2

    .line 269
    iget p2, v2, Lqmn;->h:I

    .line 270
    .line 271
    iget-object v1, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 272
    .line 273
    invoke-virtual {v1}, Landroid/widget/LinearLayout;->getHeight()I

    .line 274
    .line 275
    .line 276
    move-result v1

    .line 277
    add-int/2addr p2, v1

    .line 278
    iget v9, v2, Lqmn;->i:I

    .line 279
    .line 280
    int-to-float v7, p1

    .line 281
    int-to-float v8, p2

    .line 282
    invoke-static/range {v3 .. v9}, Lqml;->n(FFFFFFI)Z

    .line 283
    .line 284
    .line 285
    move-result p1

    .line 286
    goto :goto_1

    .line 287
    :cond_8
    move v3, p1

    .line 288
    move v4, p2

    .line 289
    iget p1, v2, Lqmn;->g:I

    .line 290
    .line 291
    int-to-float v5, p1

    .line 292
    iget p2, v2, Lqmn;->h:I

    .line 293
    .line 294
    int-to-float v6, p2

    .line 295
    iget-object p2, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 296
    .line 297
    invoke-virtual {p2}, Landroid/widget/LinearLayout;->getWidth()I

    .line 298
    .line 299
    .line 300
    move-result p2

    .line 301
    add-int/2addr p1, p2

    .line 302
    iget v8, p0, Lqml;->b:F

    .line 303
    .line 304
    iget v9, v2, Lqmn;->i:I

    .line 305
    .line 306
    int-to-float v7, p1

    .line 307
    invoke-static/range {v3 .. v9}, Lqml;->n(FFFFFFI)Z

    .line 308
    .line 309
    .line 310
    move-result p1

    .line 311
    goto :goto_1

    .line 312
    :cond_9
    move v3, p1

    .line 313
    move v4, p2

    .line 314
    iget p1, v2, Lqmn;->g:I

    .line 315
    .line 316
    int-to-float v5, p1

    .line 317
    iget v6, p0, Lqml;->b:F

    .line 318
    .line 319
    iget-object p2, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 320
    .line 321
    invoke-virtual {p2}, Landroid/widget/LinearLayout;->getWidth()I

    .line 322
    .line 323
    .line 324
    move-result p2

    .line 325
    add-int/2addr p1, p2

    .line 326
    iget p2, v2, Lqmn;->h:I

    .line 327
    .line 328
    iget-object v1, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 329
    .line 330
    invoke-virtual {v1}, Landroid/widget/LinearLayout;->getHeight()I

    .line 331
    .line 332
    .line 333
    move-result v1

    .line 334
    add-int/2addr p2, v1

    .line 335
    iget v9, v2, Lqmn;->i:I

    .line 336
    .line 337
    int-to-float v7, p1

    .line 338
    int-to-float v8, p2

    .line 339
    invoke-static/range {v3 .. v9}, Lqml;->n(FFFFFFI)Z

    .line 340
    .line 341
    .line 342
    move-result p1

    .line 343
    :goto_1
    if-eqz p1, :cond_a

    .line 344
    .line 345
    invoke-virtual {p0}, Lqml;->b()V

    .line 346
    .line 347
    .line 348
    goto/16 :goto_5

    .line 349
    .line 350
    :cond_a
    iget-boolean p1, p0, Lqml;->a:Z

    .line 351
    .line 352
    const/4 p2, 0x0

    .line 353
    if-eqz p1, :cond_b

    .line 354
    .line 355
    invoke-virtual {v0}, Lqmo;->e()F

    .line 356
    .line 357
    .line 358
    move-result p1

    .line 359
    goto :goto_2

    .line 360
    :cond_b
    move p1, p2

    .line 361
    :goto_2
    iget v0, v2, Lqmn;->b:I

    .line 362
    .line 363
    int-to-float v0, v0

    .line 364
    invoke-virtual {v2}, Lqmn;->a()Z

    .line 365
    .line 366
    .line 367
    move-result v1

    .line 368
    if-eqz v1, :cond_c

    .line 369
    .line 370
    neg-float v1, p1

    .line 371
    goto :goto_3

    .line 372
    :cond_c
    invoke-virtual {v2}, Lqmn;->d()Z

    .line 373
    .line 374
    .line 375
    move-result v1

    .line 376
    if-eqz v1, :cond_d

    .line 377
    .line 378
    move v1, p1

    .line 379
    goto :goto_3

    .line 380
    :cond_d
    move v1, p2

    .line 381
    :goto_3
    mul-float/2addr v0, v1

    .line 382
    iget v1, v2, Lqmn;->a:I

    .line 383
    .line 384
    int-to-float v1, v1

    .line 385
    iget v5, v2, Lqmn;->l:I

    .line 386
    .line 387
    const/4 v6, 0x4

    .line 388
    if-ne v5, v6, :cond_e

    .line 389
    .line 390
    neg-float p2, p1

    .line 391
    goto :goto_4

    .line 392
    :cond_e
    invoke-virtual {v2}, Lqmn;->c()Z

    .line 393
    .line 394
    .line 395
    move-result v5

    .line 396
    if-eqz v5, :cond_f

    .line 397
    .line 398
    move p2, p1

    .line 399
    :cond_f
    :goto_4
    mul-float/2addr v1, p2

    .line 400
    add-float p2, v4, v0

    .line 401
    .line 402
    invoke-static {p2}, Ljava/lang/Math;->round(F)I

    .line 403
    .line 404
    .line 405
    move-result p1

    .line 406
    iget p2, v2, Lqmn;->h:I

    .line 407
    .line 408
    sub-int/2addr p1, p2

    .line 409
    iget-object p2, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 410
    .line 411
    invoke-virtual {p2}, Landroid/widget/LinearLayout;->getPaddingTop()I

    .line 412
    .line 413
    .line 414
    move-result p2

    .line 415
    sub-int/2addr p1, p2

    .line 416
    iget-object p2, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 417
    .line 418
    invoke-virtual {p2}, Landroid/widget/LinearLayout;->getHeight()I

    .line 419
    .line 420
    .line 421
    move-result p2

    .line 422
    iget-object v0, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 423
    .line 424
    invoke-virtual {v0}, Landroid/widget/LinearLayout;->getPaddingTop()I

    .line 425
    .line 426
    .line 427
    move-result v0

    .line 428
    sub-int/2addr p2, v0

    .line 429
    iget-object v0, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 430
    .line 431
    invoke-virtual {v0}, Landroid/widget/LinearLayout;->getPaddingBottom()I

    .line 432
    .line 433
    .line 434
    move-result v0

    .line 435
    sub-int/2addr p2, v0

    .line 436
    add-float v0, v3, v1

    .line 437
    .line 438
    invoke-static {p1, p2}, Lqml;->l(II)I

    .line 439
    .line 440
    .line 441
    move-result p1

    .line 442
    invoke-static {v0}, Ljava/lang/Math;->round(F)I

    .line 443
    .line 444
    .line 445
    move-result p2

    .line 446
    iget v0, v2, Lqmn;->g:I

    .line 447
    .line 448
    sub-int/2addr p2, v0

    .line 449
    iget-object v0, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 450
    .line 451
    invoke-virtual {v0}, Landroid/widget/LinearLayout;->getPaddingLeft()I

    .line 452
    .line 453
    .line 454
    move-result v0

    .line 455
    sub-int/2addr p2, v0

    .line 456
    iget-object v0, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 457
    .line 458
    invoke-virtual {v0}, Landroid/widget/LinearLayout;->getWidth()I

    .line 459
    .line 460
    .line 461
    move-result v0

    .line 462
    iget-object v1, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 463
    .line 464
    invoke-virtual {v1}, Landroid/widget/LinearLayout;->getPaddingLeft()I

    .line 465
    .line 466
    .line 467
    move-result v1

    .line 468
    sub-int/2addr v0, v1

    .line 469
    iget-object v1, p0, Lqml;->c:Landroid/widget/LinearLayout;

    .line 470
    .line 471
    invoke-virtual {v1}, Landroid/widget/LinearLayout;->getPaddingRight()I

    .line 472
    .line 473
    .line 474
    move-result v1

    .line 475
    sub-int/2addr v0, v1

    .line 476
    invoke-static {p2, v0}, Lqml;->l(II)I

    .line 477
    .line 478
    .line 479
    move-result p2

    .line 480
    invoke-virtual {p0, p2, p1}, Lqml;->i(II)V

    :goto_5
    invoke-virtual {p0}, Lqml;->a()Lpva;

    move-result-object v1

    goto :goto_6

    :cond_10
    throw v1

    :cond_11
    iget p1, p0, Lqml;->h:I

    const/4 p2, -0x1

    if-eq p1, p2, :cond_12

    iget-object p0, p0, Lqml;->e:Ljava/util/List;

    invoke-interface {p0, p1}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object p0

    move-object v1, p0

    check-cast v1, Lpva;

    :cond_12
    :goto_6
    return-object v1
.end method
