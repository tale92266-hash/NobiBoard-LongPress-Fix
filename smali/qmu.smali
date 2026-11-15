.class public final Lqmu;
.super Lqml;
.source "PG"


# instance fields
.field public final g:Lqmr;

.field public h:Landroid/view/View;

.field private final i:Lqmt;

.field private final j:Lqme;


# direct methods
.method public constructor <init>(Landroid/content/Context;Landroid/util/AttributeSet;)V
    .locals 2

    .line 1
    new-instance v0, Lqmp;

    .line 2
    .line 3
    const/4 v1, 0x0

    .line 4
    invoke-direct {v0, v1}, Lqmp;-><init>(I)V

    .line 5
    .line 6
    .line 7
    invoke-direct {p0, p1, p2, v0}, Lqml;-><init>(Landroid/content/Context;Landroid/util/AttributeSet;Lqmk;)V

    .line 8
    .line 9
    .line 10
    new-instance p1, Lqmr;

    .line 11
    .line 12
    invoke-direct {p1, p0}, Lqmr;-><init>(Lqmu;)V

    .line 13
    .line 14
    .line 15
    iput-object p1, p0, Lqmu;->g:Lqmr;

    .line 16
    .line 17
    new-instance p2, Lqmt;

    .line 18
    .line 19
    invoke-direct {p2, p0}, Lqmt;-><init>(Lqmu;)V

    .line 20
    .line 21
    .line 22
    iput-object p2, p0, Lqmu;->i:Lqmt;

    .line 23
    .line 24
    new-instance v0, Lqme;

    .line 25
    .line 26
    invoke-direct {v0, p1, p2}, Lqme;-><init>(Lqmd;Lqmt;)V

    .line 27
    .line 28
    .line 29
    iput-object v0, p0, Lqmu;->j:Lqme;

    .line 30
    .line 31
    return-void
.end method

.method public static l(Landroid/view/View;)F
    .locals 8

    .line 1
    instance-of v0, p0, Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;

    .line 2
    .line 3
    const v1, 0x3fa8f5c3    # 1.32f

    .line 4
    .line 5
    .line 6
    if-eqz v0, :cond_1

    .line 7
    .line 8
    check-cast p0, Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;

    .line 9
    .line 10
    iget v0, p0, Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;->g:F

    .line 11
    .line 12
    const/4 v2, 0x0

    .line 13
    cmpl-float v2, v0, v2

    .line 14
    .line 15
    if-nez v2, :cond_0

    .line 16
    .line 17
    invoke-virtual {p0}, Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;->getMeasuredWidth()I

    .line 18
    .line 19
    .line 20
    move-result v0

    .line 21
    invoke-virtual {p0}, Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;->getMeasuredHeight()I

    .line 22
    .line 23
    .line 24
    move-result v2

    .line 25
    invoke-virtual {p0}, Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;->d()Landroid/graphics/Rect;

    .line 26
    .line 27
    .line 28
    move-result-object v3

    .line 29
    new-instance v4, Landroid/graphics/Rect;

    .line 30
    .line 31
    invoke-virtual {p0}, Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;->getPaddingLeft()I

    .line 32
    .line 33
    .line 34
    move-result v5

    .line 35
    invoke-virtual {p0}, Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;->getPaddingTop()I

    .line 36
    .line 37
    .line 38
    move-result v6

    .line 39
    invoke-virtual {p0}, Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;->getPaddingRight()I

    .line 40
    .line 41
    .line 42
    move-result v7

    .line 43
    sub-int/2addr v0, v7

    .line 44
    invoke-virtual {p0}, Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;->getPaddingBottom()I

    .line 45
    .line 46
    .line 47
    move-result v7

    .line 48
    sub-int/2addr v2, v7

    .line 49
    invoke-direct {v4, v5, v6, v0, v2}, Landroid/graphics/Rect;-><init>(IIII)V

    .line 50
    .line 51
    .line 52
    invoke-virtual {v4}, Landroid/graphics/Rect;->width()I

    .line 53
    .line 54
    .line 55
    move-result v0

    .line 56
    int-to-float v0, v0

    .line 57
    invoke-virtual {v3}, Landroid/graphics/Rect;->width()I

    .line 58
    .line 59
    .line 60
    move-result v2

    .line 61
    int-to-float v2, v2

    .line 62
    invoke-virtual {v4}, Landroid/graphics/Rect;->height()I

    .line 63
    .line 64
    .line 65
    move-result v4

    .line 66
    int-to-float v4, v4

    .line 67
    invoke-virtual {v3}, Landroid/graphics/Rect;->height()I

    .line 68
    .line 69
    .line 70
    move-result v3

    .line 71
    int-to-float v3, v3

    .line 72
    div-float/2addr v0, v2

    .line 73
    div-float/2addr v4, v3

    .line 74
    invoke-static {v0, v4}, Ljava/lang/Math;->min(FF)F

    .line 75
    .line 76
    .line 77
    move-result v0

    .line 78
    iput v0, p0, Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;->g:F

    .line 79
    .line 80
    :cond_0
    const/high16 p0, 0x3f800000    # 1.0f

    .line 81
    .line 82
    invoke-static {v0, v1}, Ljava/lang/Math;->min(FF)F

    .line 83
    .line 84
    .line 85
    move-result v0

    .line 86
    invoke-static {p0, v0}, Ljava/lang/Math;->max(FF)F

    .line 87
    .line 88
    .line 89
    move-result v1

    .line 90
    :cond_1
    return v1
.end method

.method private final o(Lqmc;)V
    .locals 6

    .line 1
    iget-object v0, p0, Lqmu;->h:Landroid/view/View;

    .line 2
    .line 3
    if-eqz v0, :cond_2

    .line 4
    .line 5
    iget-object p1, p1, Lqmc;->a:Landroid/graphics/Point;

    .line 6
    .line 7
    iget-object v1, p0, Lqmu;->g:Lqmr;

    .line 8
    .line 9
    iget v2, p1, Landroid/graphics/Point;->x:I

    .line 10
    .line 11
    invoke-virtual {v1}, Lqmr;->b()I

    .line 12
    .line 13
    .line 14
    move-result v3

    .line 15
    mul-int/2addr v2, v3

    .line 16
    const/4 v5, 0x0

    .line 17
    invoke-virtual {v0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    .line 18
    .line 19
    .line 20
    move-result-object v4

    .line 21
    if-eqz v4, :cond_0

    .line 22
    .line 23
    iget v5, v4, Landroid/view/ViewGroup$LayoutParams;->width:I

    .line 24
    .line 25
    if-lez v5, :cond_0

    .line 26
    .line 27
    sub-int v5, v3, v5

    .line 28
    .line 29
    div-int/lit8 v5, v5, 0x2

    .line 30
    .line 31
    add-int/2addr v2, v5

    .line 32
    :cond_0
    int-to-float v2, v2

    .line 33
    invoke-virtual {v0, v2}, Landroid/view/View;->setTranslationX(F)V

    .line 34
    .line 35
    .line 36
    iget-object v0, p0, Lqmu;->h:Landroid/view/View;

    .line 37
    .line 38
    iget v5, p1, Landroid/graphics/Point;->y:I

    .line 39
    .line 40
    invoke-virtual {v1}, Lqmr;->a()I

    .line 41
    .line 42
    .line 43
    move-result v1

    .line 44
    mul-int/2addr v5, v1

    .line 45
    invoke-virtual {v0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    .line 46
    .line 47
    .line 48
    move-result-object v4

    .line 49
    if-eqz v4, :cond_1

    .line 50
    .line 51
    iget v3, v4, Landroid/view/ViewGroup$LayoutParams;->height:I

    .line 52
    .line 53
    if-lez v3, :cond_1

    .line 54
    .line 55
    sub-int v3, v1, v3

    .line 56
    .line 57
    div-int/lit8 v3, v3, 0x2

    .line 58
    .line 59
    add-int/2addr v5, v3

    .line 60
    :cond_1
    int-to-float v5, v5

    .line 61
    invoke-virtual {v0, v5}, Landroid/view/View;->setTranslationY(F)V

    .line 62
    .line 63
    .line 64
    iget-object v5, p0, Lqmu;->h:Landroid/view/View;

    .line 65
    .line 66
    const/high16 v0, 0x3f800000    # 1.0f

    .line 67
    .line 68
    invoke-virtual {v5, v0}, Landroid/view/View;->setScaleX(F)V

    .line 69
    .line 70
    .line 71
    iget-object v5, p0, Lqmu;->h:Landroid/view/View;

    .line 72
    .line 73
    invoke-virtual {v5, v0}, Landroid/view/View;->setScaleY(F)V

    .line 74
    iget-object v4, p0, Lqmu;->h:Landroid/view/View;

    invoke-virtual {v4, v0}, Landroid/view/View;->setAlpha(F)V

    :cond_2
    return-void
.end method

.method private final p()V
    .locals 2

    .line 1
    iget-object v0, p0, Lqmu;->g:Lqmr;

    .line 2
    .line 3
    iget-object v1, v0, Lqmr;->b:Landroid/graphics/Point;

    .line 4
    .line 5
    invoke-static {v0, v1}, Lmgw;->L(Lqmd;Landroid/graphics/Point;)V

    .line 6
    .line 7
    .line 8
    invoke-static {v0, v1}, Lmgw;->I(Lqmd;Landroid/graphics/Point;)Lqmc;

    .line 9
    .line 10
    .line 11
    move-result-object v0

    .line 12
    if-eqz v0, :cond_1

    .line 13
    .line 14
    invoke-virtual {p0, v0}, Lqmu;->m(Lqmc;)V

    .line 15
    .line 16
    .line 17
    iget-boolean v1, p0, Lqmu;->a:Z

    .line 18
    .line 19
    if-eqz v1, :cond_0

    .line 20
    .line 21
    invoke-direct {p0, v0}, Lqmu;->o(Lqmc;)V

    .line 22
    .line 23
    .line 24
    iget-object p0, p0, Lqmu;->h:Landroid/view/View;

    .line 25
    .line 26
    if-eqz p0, :cond_1

    .line 27
    .line 28
    const/4 v0, 0x0

    .line 29
    invoke-virtual {p0, v0}, Landroid/view/View;->setVisibility(I)V

    .line 30
    .line 31
    .line 32
    goto :goto_0

    .line 33
    :cond_0
    iget-object p0, p0, Lqmu;->h:Landroid/view/View;

    .line 34
    .line 35
    if-eqz p0, :cond_1

    .line 36
    .line 37
    const/4 v0, 0x4

    .line 38
    invoke-virtual {p0, v0}, Landroid/view/View;->setVisibility(I)V

    .line 39
    .line 40
    .line 41
    :cond_1
    :goto_0
    return-void
.end method


# virtual methods
.method public final a()Lpva;
    .locals 2

    .line 1
    iget-object v0, p0, Lqmu;->g:Lqmr;

    .line 2
    .line 3
    invoke-static {v0}, Lmgw;->K(Lqmd;)Lqmc;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    if-eqz v0, :cond_0

    .line 8
    .line 9
    iget v0, v0, Lqmc;->b:I

    .line 10
    .line 11
    if-ltz v0, :cond_0

    .line 12
    .line 13
    iget-object v1, p0, Lqml;->e:Ljava/util/List;

    .line 14
    .line 15
    invoke-interface {v1}, Ljava/util/List;->size()I

    .line 16
    .line 17
    .line 18
    move-result v1

    .line 19
    if-ge v0, v1, :cond_0

    .line 20
    .line 21
    iget-object p0, p0, Lqml;->e:Ljava/util/List;

    .line 22
    .line 23
    invoke-interface {p0, v0}, Ljava/util/List;->get(I)Ljava/lang/Object;

    .line 24
    .line 25
    .line 26
    move-result-object p0

    .line 27
    check-cast p0, Lpva;

    .line 28
    .line 29
    goto :goto_0

    .line 30
    :cond_0
    const/4 p0, 0x0

    .line 31
    :goto_0
    return-object p0
.end method

.method public final b()V
    .locals 3

    .line 1
    invoke-super {p0}, Lqml;->b()V

    .line 2
    .line 3
    .line 4
    iget-object v0, p0, Lqmu;->g:Lqmr;

    .line 5
    .line 6
    invoke-static {v0}, Lmgw;->K(Lqmd;)Lqmc;

    .line 7
    .line 8
    .line 9
    move-result-object v1

    .line 10
    sget-object v2, Lqmd;->a:Landroid/graphics/Point;

    .line 11
    .line 12
    invoke-static {v0, v2}, Lmgw;->L(Lqmd;Landroid/graphics/Point;)V

    .line 13
    .line 14
    .line 15
    invoke-virtual {p0, v1}, Lqmu;->m(Lqmc;)V

    .line 16
    .line 17
    .line 18
    iget-object p0, p0, Lqmu;->h:Landroid/view/View;

    .line 19
    .line 20
    if-eqz p0, :cond_0

    .line 21
    .line 22
    const/4 v0, 0x4

    .line 23
    invoke-virtual {p0, v0}, Landroid/view/View;->setVisibility(I)V

    .line 24
    .line 25
    .line 26
    :cond_0
    return-void
.end method

.method public final c(Landroid/widget/LinearLayout;Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;Landroid/view/View;FFLcom/google/android/libraries/inputmethod/metadata/ActionDef;[I)V
    .locals 6

    .line 1
    invoke-super/range {p0 .. p7}, Lqml;->c(Landroid/widget/LinearLayout;Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;Landroid/view/View;FFLcom/google/android/libraries/inputmethod/metadata/ActionDef;[I)V

    .line 2
    .line 3
    .line 4
    iget-object p2, p0, Lqmu;->f:Lqmo;

    .line 5
    .line 6
    iget-object p2, p2, Lqmo;->d:Lqmn;

    .line 7
    .line 8
    iget p3, p2, Lqmn;->f:I

    .line 9
    .line 10
    invoke-virtual {p2}, Lqmn;->d()Z

    .line 11
    .line 12
    .line 13
    move-result p4

    .line 14
    const/4 p5, -0x1

    .line 15
    const/4 p6, 0x0

    .line 16
    if-nez p4, :cond_0

    .line 17
    .line 18
    invoke-virtual {p2}, Lqmn;->c()Z

    .line 19
    .line 20
    .line 21
    move-result p4

    .line 22
    if-nez p4, :cond_0

    .line 23
    .line 24
    iget p2, p2, Lqmn;->d:I

    .line 25
    .line 26
    add-int/2addr p2, p5

    .line 27
    goto :goto_0

    .line 28
    :cond_0
    move p2, p6

    .line 29
    :goto_0
    iget-object p4, p0, Lqmu;->g:Lqmr;

    .line 30
    .line 31
    iget-object p7, p4, Lqmr;->b:Landroid/graphics/Point;

    .line 32
    .line 33
    invoke-virtual {p7, p3, p2}, Landroid/graphics/Point;->set(II)V

    .line 34
    .line 35
    .line 36
    iget-object p7, p4, Lqmr;->c:Landroid/graphics/Point;

    .line 37
    .line 38
    invoke-virtual {p7, p3, p2}, Landroid/graphics/Point;->set(II)V

    .line 39
    .line 40
    .line 41
    iget-object p2, p4, Lqmr;->d:Ljava/util/List;

    .line 42
    .line 43
    invoke-interface {p2}, Ljava/util/List;->clear()V

    .line 44
    .line 45
    .line 46
    move p3, p6

    .line 47
    :goto_1
    invoke-virtual {p4}, Lqmr;->d()I

    .line 48
    .line 49
    .line 50
    move-result p7

    .line 51
    if-ge p3, p7, :cond_6

    .line 52
    .line 53
    invoke-virtual {p1, p3}, Landroid/widget/LinearLayout;->getChildAt(I)Landroid/view/View;

    .line 54
    .line 55
    .line 56
    move-result-object p7

    .line 57
    check-cast p7, Landroid/view/ViewGroup;

    .line 58
    .line 59
    move v0, p6

    .line 60
    :goto_2
    invoke-virtual {p4}, Lqmr;->c()I

    .line 61
    .line 62
    .line 63
    move-result v1

    .line 64
    if-ge v0, v1, :cond_5

    .line 65
    .line 66
    invoke-virtual {p7, v0}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    .line 67
    .line 68
    .line 69
    move-result-object v1

    .line 70
    invoke-virtual {v1}, Landroid/view/View;->getId()I

    .line 71
    .line 72
    .line 73
    move-result v2

    .line 74
    if-eq v2, p5, :cond_4

    .line 75
    .line 76
    new-instance v2, Landroid/graphics/Point;

    .line 77
    .line 78
    invoke-direct {v2, v0, p3}, Landroid/graphics/Point;-><init>(II)V

    .line 79
    .line 80
    .line 81
    invoke-virtual {v1}, Landroid/view/View;->getId()I

    .line 82
    .line 83
    .line 84
    move-result v3

    .line 85
    if-eqz v1, :cond_3

    .line 86
    .line 87
    const v4, 0x7f0b070b

    .line 88
    .line 89
    .line 90
    invoke-virtual {v1, v4}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 91
    .line 92
    .line 93
    move-result-object v4

    .line 94
    if-nez v4, :cond_1

    .line 95
    .line 96
    const v4, 0x7f0b070a

    .line 97
    .line 98
    .line 99
    invoke-virtual {v1, v4}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 100
    .line 101
    .line 102
    move-result-object v4

    .line 103
    :cond_1
    if-eqz v4, :cond_2

    .line 104
    .line 105
    new-instance v5, Lqmc;

    .line 106
    .line 107
    invoke-direct {v5, v2, v3, v1, v4}, Lqmc;-><init>(Landroid/graphics/Point;ILandroid/view/View;Landroid/view/View;)V

    .line 108
    .line 109
    .line 110
    invoke-interface {p2, v5}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 111
    .line 112
    .line 113
    goto :goto_3

    .line 114
    :cond_2
    new-instance p0, Ljava/lang/NullPointerException;

    .line 115
    .line 116
    const-string p1, "Null labelView"

    .line 117
    .line 118
    invoke-direct {p0, p1}, Ljava/lang/NullPointerException;-><init>(Ljava/lang/String;)V

    .line 119
    .line 120
    .line 121
    throw p0

    .line 122
    :cond_3
    new-instance p0, Ljava/lang/NullPointerException;

    .line 123
    .line 124
    const-string p1, "Null view"

    .line 125
    .line 126
    invoke-direct {p0, p1}, Ljava/lang/NullPointerException;-><init>(Ljava/lang/String;)V

    .line 127
    .line 128
    .line 129
    throw p0

    .line 130
    :cond_4
    const/4 v1, 0x0

    .line 131
    invoke-interface {p2, v1}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 132
    .line 133
    .line 134
    :goto_3
    add-int/lit8 v0, v0, 0x1

    .line 135
    .line 136
    goto :goto_2

    .line 137
    :cond_5
    add-int/lit8 p3, p3, 0x1

    .line 138
    .line 139
    goto :goto_1

    .line 140
    :cond_6
    iget-object v0, p0, Lqml;->e:Ljava/util/List;

    .line 141
    .line 142
    if-eqz v0, :cond_7

    .line 143
    .line 144
    invoke-interface {v0}, Ljava/util/List;->size()I

    .line 145
    .line 146
    .line 147
    move-result v1

    .line 148
    const/16 v2, 0x9

    .line 149
    .line 150
    if-ne v1, v2, :cond_7

    .line 151
    .line 152
    const/4 v1, 0x3

    .line 153
    invoke-interface {v0, v1}, Ljava/util/List;->get(I)Ljava/lang/Object;

    .line 154
    .line 155
    .line 156
    move-result-object v1

    .line 157
    instance-of v2, v1, Lpva;

    .line 158
    .line 159
    if-eqz v2, :cond_7

    .line 160
    .line 161
    iget-object v2, v1, Lpva;->e:Ljava/lang/Object;

    .line 162
    .line 163
    instance-of v3, v2, Ljava/lang/CharSequence;

    .line 164
    .line 165
    if-eqz v3, :cond_7

    .line 166
    .line 167
    invoke-interface {v2}, Ljava/lang/CharSequence;->length()I

    .line 168
    .line 169
    .line 170
    move-result v3

    .line 171
    if-eqz v3, :cond_7

    .line 172
    .line 173
    const/4 v3, 0x0

    .line 174
    invoke-static {v2, v3}, Ljava/lang/Character;->codePointAt(Ljava/lang/CharSequence;I)I

    .line 175
    .line 176
    .line 177
    move-result v3

    .line 178
    const/high16 v2, 0x10000

    .line 179
    .line 180
    if-lt v3, v2, :cond_7

    .line 181
    .line 182
    iget-object v0, p0, Lqmu;->g:Lqmr;

    .line 183
    .line 184
    iget-object v1, v0, Lqmr;->b:Landroid/graphics/Point;

    .line 185
    .line 186
    const/4 v2, 0x1

    .line 187
    const/4 v3, 0x1

    .line 188
    invoke-virtual {v1, v2, v3}, Landroid/graphics/Point;->set(II)V

    .line 189
    .line 190
    .line 191
    iget-object v1, v0, Lqmr;->c:Landroid/graphics/Point;

    .line 192
    .line 193
    invoke-virtual {v1, v2, v3}, Landroid/graphics/Point;->set(II)V

    .line 194
    .line 195
    .line 196
    :cond_7
    invoke-direct {p0}, Lqmu;->p()V

    .line 197
    .line 198
    .line 199
    return-void
.end method

.method public final d()V
    .locals 4

    .line 1
    invoke-super {p0}, Lqml;->d()V

    .line 2
    .line 3
    .line 4
    sget-object v0, Lqmr;->a:Landroid/graphics/Point;

    .line 5
    .line 6
    iget v1, v0, Landroid/graphics/Point;->x:I

    .line 7
    .line 8
    iget v2, v0, Landroid/graphics/Point;->y:I

    .line 9
    .line 10
    iget-object p0, p0, Lqmu;->g:Lqmr;

    .line 11
    .line 12
    iget-object v3, p0, Lqmr;->b:Landroid/graphics/Point;

    .line 13
    .line 14
    invoke-virtual {v3, v1, v2}, Landroid/graphics/Point;->set(II)V

    .line 15
    .line 16
    .line 17
    iget v1, v0, Landroid/graphics/Point;->x:I

    .line 18
    .line 19
    iget v0, v0, Landroid/graphics/Point;->y:I

    .line 20
    .line 21
    iget-object v2, p0, Lqmr;->c:Landroid/graphics/Point;

    .line 22
    .line 23
    invoke-virtual {v2, v1, v0}, Landroid/graphics/Point;->set(II)V

    .line 24
    .line 25
    .line 26
    iget-object p0, p0, Lqmr;->d:Ljava/util/List;

    .line 27
    .line 28
    invoke-interface {p0}, Ljava/util/List;->clear()V

    .line 29
    .line 30
    .line 31
    return-void
.end method

.method public final e()V
    .locals 0

    .line 1
    invoke-super {p0}, Lqml;->e()V

    .line 2
    .line 3
    .line 4
    invoke-direct {p0}, Lqmu;->p()V

    .line 5
    .line 6
    .line 7
    return-void
.end method

.method protected final i(II)V
    .locals 24

    .line 1
    move-object/from16 v0, p0

    .line 2
    .line 3
    move/from16 v1, p1

    .line 4
    .line 5
    move/from16 v2, p2

    .line 6
    .line 7
    iget-object v3, v0, Lqmu;->g:Lqmr;

    .line 8
    .line 9
    invoke-static {v3}, Lmgw;->K(Lqmd;)Lqmc;

    .line 10
    .line 11
    .line 12
    move-result-object v4

    .line 13
    const/4 v5, 0x0

    .line 14
    move v9, v5

    .line 15
    const-wide/16 v10, 0x0

    .line 16
    .line 17
    const/4 v12, 0x0

    .line 18
    const/4 v13, 0x0

    .line 19
    :goto_0
    iget-object v14, v0, Lqmu;->j:Lqme;

    .line 20
    .line 21
    const-wide/16 v16, 0x0

    .line 22
    .line 23
    if-nez v9, :cond_d

    .line 24
    .line 25
    iget-object v7, v14, Lqme;->c:Lqmd;

    .line 26
    .line 27
    invoke-static {v7}, Lmgw;->K(Lqmd;)Lqmc;

    .line 28
    .line 29
    .line 30
    move-result-object v12

    .line 31
    if-nez v12, :cond_0

    .line 32
    .line 33
    move v15, v5

    .line 34
    move-wide/from16 v10, v16

    .line 35
    .line 36
    const/4 v8, 0x0

    .line 37
    const/16 v19, 0x3

    .line 38
    .line 39
    const/16 v21, 0x1

    .line 40
    .line 41
    goto/16 :goto_6

    .line 42
    .line 43
    :cond_0
    invoke-static {v7}, Lmgw;->K(Lqmd;)Lqmc;

    .line 44
    .line 45
    .line 46
    move-result-object v10

    .line 47
    if-nez v10, :cond_1

    .line 48
    .line 49
    move v15, v5

    .line 50
    const/16 v18, -0x1

    .line 51
    .line 52
    const/16 v19, 0x3

    .line 53
    .line 54
    const/16 v21, 0x1

    .line 55
    .line 56
    goto/16 :goto_3

    .line 57
    .line 58
    :cond_1
    invoke-interface {v7}, Lqmd;->b()I

    .line 59
    .line 60
    .line 61
    move-result v13

    .line 62
    int-to-float v13, v13

    .line 63
    const/high16 v18, 0x3f000000    # 0.5f

    .line 64
    .line 65
    mul-float v13, v13, v18

    .line 66
    .line 67
    invoke-static {v13}, Ljava/lang/Math;->round(F)I

    .line 68
    .line 69
    .line 70
    move-result v13

    .line 71
    invoke-interface {v7}, Lqmd;->a()I

    .line 72
    .line 73
    .line 74
    move-result v8

    .line 75
    int-to-float v8, v8

    .line 76
    mul-float v8, v8, v18

    .line 77
    .line 78
    invoke-static {v8}, Ljava/lang/Math;->round(F)I

    .line 79
    .line 80
    .line 81
    move-result v8

    .line 82
    const/16 v18, -0x1

    .line 83
    .line 84
    invoke-interface {v7}, Lqmd;->c()I

    .line 85
    .line 86
    .line 87
    move-result v11

    .line 88
    int-to-float v11, v11

    .line 89
    const/16 v19, 0x3

    .line 90
    .line 91
    invoke-interface {v7}, Lqmd;->b()I

    .line 92
    .line 93
    .line 94
    move-result v15

    .line 95
    int-to-float v15, v15

    .line 96
    const/high16 v20, -0x41000000    # -0.5f

    .line 97
    .line 98
    add-float v11, v11, v20

    .line 99
    .line 100
    mul-float/2addr v11, v15

    .line 101
    invoke-static {v11}, Ljava/lang/Math;->round(F)I

    .line 102
    .line 103
    .line 104
    move-result v11

    .line 105
    invoke-interface {v7}, Lqmd;->d()I

    .line 106
    .line 107
    .line 108
    move-result v15

    .line 109
    int-to-float v15, v15

    .line 110
    const/16 v21, 0x1

    .line 111
    .line 112
    invoke-interface {v7}, Lqmd;->a()I

    .line 113
    .line 114
    .line 115
    move-result v6

    .line 116
    int-to-float v6, v6

    .line 117
    add-float v15, v15, v20

    .line 118
    .line 119
    mul-float/2addr v15, v6

    .line 120
    invoke-static {v15}, Ljava/lang/Math;->round(F)I

    .line 121
    .line 122
    .line 123
    move-result v6

    .line 124
    new-instance v15, Landroid/graphics/Point;

    .line 125
    .line 126
    invoke-static {v1, v13, v11}, Lsag;->e(III)I

    .line 127
    .line 128
    .line 129
    move-result v11

    .line 130
    invoke-static {v2, v8, v6}, Lsag;->e(III)I

    .line 131
    .line 132
    .line 133
    move-result v6

    .line 134
    invoke-direct {v15, v11, v6}, Landroid/graphics/Point;-><init>(II)V

    .line 135
    .line 136
    .line 137
    iget v6, v15, Landroid/graphics/Point;->x:I

    .line 138
    .line 139
    iget v8, v15, Landroid/graphics/Point;->y:I

    .line 140
    .line 141
    iget-object v10, v10, Lqmc;->a:Landroid/graphics/Point;

    .line 142
    .line 143
    invoke-virtual {v14, v6, v8, v10}, Lqme;->a(IILandroid/graphics/Point;)Landroid/graphics/Point;

    .line 144
    .line 145
    .line 146
    move-result-object v6

    .line 147
    invoke-virtual {v6, v5, v5}, Landroid/graphics/Point;->equals(II)Z

    .line 148
    .line 149
    .line 150
    move-result v8

    .line 151
    if-eqz v8, :cond_2

    .line 152
    .line 153
    move v15, v5

    .line 154
    goto :goto_3

    .line 155
    :cond_2
    iget v8, v6, Landroid/graphics/Point;->y:I

    .line 156
    .line 157
    invoke-static {v8}, Ljava/lang/Math;->abs(I)I

    .line 158
    .line 159
    .line 160
    move-result v8

    .line 161
    int-to-double v10, v8

    .line 162
    iget v8, v6, Landroid/graphics/Point;->x:I

    .line 163
    .line 164
    invoke-static {v8}, Ljava/lang/Math;->abs(I)I

    .line 165
    .line 166
    .line 167
    move-result v8

    .line 168
    move v15, v5

    .line 169
    move-object v13, v6

    .line 170
    int-to-double v5, v8

    .line 171
    sget-wide v22, Lqme;->b:D

    .line 172
    .line 173
    mul-double v5, v5, v22

    .line 174
    .line 175
    cmpg-double v5, v10, v5

    .line 176
    .line 177
    if-gez v5, :cond_4

    .line 178
    .line 179
    iget v5, v13, Landroid/graphics/Point;->x:I

    .line 180
    .line 181
    if-ltz v5, :cond_3

    .line 182
    .line 183
    move/from16 v5, v21

    .line 184
    .line 185
    goto :goto_1

    .line 186
    :cond_3
    move/from16 v5, v18

    .line 187
    .line 188
    goto :goto_1

    .line 189
    :cond_4
    move v5, v15

    .line 190
    :goto_1
    iget v6, v13, Landroid/graphics/Point;->y:I

    .line 191
    .line 192
    invoke-static {v6}, Ljava/lang/Math;->abs(I)I

    .line 193
    .line 194
    .line 195
    move-result v6

    .line 196
    int-to-double v10, v6

    .line 197
    iget v6, v13, Landroid/graphics/Point;->x:I

    .line 198
    .line 199
    invoke-static {v6}, Ljava/lang/Math;->abs(I)I

    .line 200
    .line 201
    .line 202
    move-result v6

    .line 203
    move v8, v5

    .line 204
    int-to-double v5, v6

    .line 205
    sget-wide v22, Lqme;->a:D

    .line 206
    .line 207
    mul-double v5, v5, v22

    .line 208
    .line 209
    cmpl-double v5, v10, v5

    .line 210
    .line 211
    if-lez v5, :cond_6

    .line 212
    .line 213
    iget v5, v13, Landroid/graphics/Point;->y:I

    .line 214
    .line 215
    if-ltz v5, :cond_5

    .line 216
    .line 217
    move/from16 v5, v19

    .line 218
    .line 219
    goto :goto_2

    .line 220
    :cond_5
    const/4 v5, -0x3

    .line 221
    :goto_2
    add-int/2addr v5, v8

    .line 222
    goto :goto_3

    .line 223
    :cond_6
    move v5, v8

    .line 224
    :goto_3
    if-nez v5, :cond_7

    .line 225
    .line 226
    const/4 v13, 0x0

    .line 227
    goto :goto_4

    .line 228
    :cond_7
    iget-object v6, v12, Lqmc;->a:Landroid/graphics/Point;

    .line 229
    .line 230
    add-int/lit8 v8, v5, 0x4

    .line 231
    .line 232
    rem-int/lit8 v10, v8, 0x3

    .line 233
    .line 234
    add-int/lit8 v10, v10, -0x1

    .line 235
    .line 236
    iget v11, v6, Landroid/graphics/Point;->x:I

    .line 237
    .line 238
    add-int/2addr v11, v10

    .line 239
    div-int/lit8 v8, v8, 0x3

    .line 240
    .line 241
    add-int/lit8 v8, v8, -0x1

    .line 242
    .line 243
    iget v6, v6, Landroid/graphics/Point;->y:I

    .line 244
    .line 245
    add-int/2addr v6, v8

    .line 246
    invoke-static {v7, v11, v6}, Lmgw;->J(Lqmd;II)Lqmc;

    .line 247
    .line 248
    .line 249
    move-result-object v6

    .line 250
    move-object v13, v6

    .line 251
    :goto_4
    if-eqz v5, :cond_c

    .line 252
    .line 253
    if-eqz v13, :cond_c

    .line 254
    .line 255
    iget-object v5, v12, Lqmc;->a:Landroid/graphics/Point;

    .line 256
    .line 257
    iget-object v6, v13, Lqmc;->a:Landroid/graphics/Point;

    .line 258
    .line 259
    invoke-virtual {v5, v6}, Landroid/graphics/Point;->equals(Ljava/lang/Object;)Z

    .line 260
    .line 261
    .line 262
    move-result v8

    .line 263
    if-nez v8, :cond_8

    .line 264
    .line 265
    iget v8, v6, Landroid/graphics/Point;->x:I

    .line 266
    .line 267
    iget v10, v5, Landroid/graphics/Point;->x:I

    .line 268
    .line 269
    sub-int/2addr v8, v10

    .line 270
    invoke-interface {v7}, Lqmd;->b()I

    .line 271
    .line 272
    .line 273
    move-result v10

    .line 274
    mul-int/2addr v8, v10

    .line 275
    iget v10, v6, Landroid/graphics/Point;->y:I

    .line 276
    .line 277
    iget v11, v5, Landroid/graphics/Point;->y:I

    .line 278
    .line 279
    sub-int/2addr v10, v11

    .line 280
    invoke-interface {v7}, Lqmd;->a()I

    .line 281
    .line 282
    .line 283
    move-result v11

    .line 284
    mul-int/2addr v10, v11

    .line 285
    invoke-virtual {v14, v1, v2, v5}, Lqme;->a(IILandroid/graphics/Point;)Landroid/graphics/Point;

    .line 286
    .line 287
    .line 288
    move-result-object v5

    .line 289
    iget v11, v5, Landroid/graphics/Point;->x:I

    .line 290
    .line 291
    mul-int/2addr v11, v8

    .line 292
    iget v5, v5, Landroid/graphics/Point;->y:I

    .line 293
    .line 294
    mul-int/2addr v5, v10

    .line 295
    sget v14, Lsag;->a:I

    .line 296
    .line 297
    mul-int/2addr v8, v8

    .line 298
    mul-int/2addr v10, v10

    .line 299
    add-int/2addr v8, v10

    .line 300
    add-int/2addr v11, v5

    .line 301
    int-to-double v10, v11

    .line 302
    int-to-double v1, v8

    .line 303
    div-double/2addr v10, v1

    .line 304
    cmpg-double v1, v10, v16

    .line 305
    .line 306
    if-gez v1, :cond_9

    .line 307
    .line 308
    :cond_8
    move-wide/from16 v10, v16

    .line 309
    .line 310
    goto :goto_5

    .line 311
    :cond_9
    const-wide/high16 v1, 0x3ff0000000000000L    # 1.0

    .line 312
    .line 313
    cmpl-double v5, v10, v1

    .line 314
    .line 315
    if-lez v5, :cond_a

    .line 316
    .line 317
    move-wide v10, v1

    .line 318
    :cond_a
    :goto_5
    const-wide/high16 v1, 0x3fe0000000000000L    # 0.5

    .line 319
    .line 320
    cmpl-double v1, v10, v1

    .line 321
    .line 322
    if-lez v1, :cond_b

    .line 323
    .line 324
    invoke-static {v7, v6}, Lmgw;->L(Lqmd;Landroid/graphics/Point;)V

    .line 325
    .line 326
    .line 327
    move/from16 v1, p1

    .line 328
    .line 329
    move/from16 v2, p2

    .line 330
    .line 331
    move v5, v15

    .line 332
    goto/16 :goto_0

    .line 333
    .line 334
    :cond_b
    move/from16 v1, p1

    .line 335
    .line 336
    move/from16 v2, p2

    .line 337
    .line 338
    move v5, v15

    .line 339
    move/from16 v9, v21

    .line 340
    .line 341
    goto/16 :goto_0

    .line 342
    .line 343
    :cond_c
    move-object v8, v13

    .line 344
    move-wide/from16 v10, v16

    .line 345
    .line 346
    goto :goto_6

    .line 347
    :cond_d
    move v15, v5

    .line 348
    const/16 v19, 0x3

    .line 349
    .line 350
    const/16 v21, 0x1

    .line 351
    .line 352
    move-object v8, v13

    .line 353
    :goto_6
    if-eqz v12, :cond_13

    .line 354
    .line 355
    if-eqz v8, :cond_13

    .line 356
    .line 357
    cmpg-double v1, v10, v16

    .line 358
    .line 359
    if-lez v1, :cond_13

    .line 360
    .line 361
    iget-object v1, v14, Lqme;->d:Lqmt;

    .line 362
    .line 363
    iget-object v2, v1, Lqmt;->a:Lqmc;

    .line 364
    .line 365
    if-ne v2, v12, :cond_e

    .line 366
    .line 367
    iget-object v2, v1, Lqmt;->b:Lqmc;

    .line 368
    .line 369
    if-ne v2, v8, :cond_e

    .line 370
    .line 371
    iget-object v2, v1, Lqmt;->c:Landroid/animation/AnimatorSet;

    .line 372
    .line 373
    if-eqz v2, :cond_e

    .line 374
    .line 375
    invoke-virtual {v2}, Landroid/animation/AnimatorSet;->isPaused()Z

    .line 376
    .line 377
    .line 378
    move-result v2

    .line 379
    if-eqz v2, :cond_e

    .line 380
    .line 381
    move-object/from16 v17, v3

    .line 382
    .line 383
    goto/16 :goto_7

    .line 384
    .line 385
    :cond_e
    invoke-virtual {v14}, Lqme;->b()V

    .line 386
    .line 387
    .line 388
    iput-object v12, v1, Lqmt;->a:Lqmc;

    .line 389
    .line 390
    iput-object v8, v1, Lqmt;->b:Lqmc;

    .line 391
    .line 392
    new-instance v2, Landroid/animation/AnimatorSet;

    .line 393
    .line 394
    invoke-direct {v2}, Landroid/animation/AnimatorSet;-><init>()V

    .line 395
    .line 396
    .line 397
    iget-object v5, v12, Lqmc;->d:Landroid/view/View;

    .line 398
    .line 399
    iget-object v6, v8, Lqmc;->d:Landroid/view/View;

    .line 400
    .line 401
    invoke-static {v5}, Lqmu;->l(Landroid/view/View;)F

    .line 402
    .line 403
    .line 404
    move-result v7

    .line 405
    invoke-static {v6}, Lqmu;->l(Landroid/view/View;)F

    .line 406
    .line 407
    .line 408
    move-result v9

    .line 409
    new-instance v13, Landroid/animation/AnimatorSet;

    .line 410
    .line 411
    invoke-direct {v13}, Landroid/animation/AnimatorSet;-><init>()V

    .line 412
    .line 413
    .line 414
    const/4 v14, 0x4

    .line 415
    new-array v14, v14, [Landroid/animation/Animator;

    .line 416
    .line 417
    move/from16 v16, v15

    .line 418
    .line 419
    sget-object v15, Landroid/view/View;->SCALE_X:Landroid/util/Property;

    .line 420
    .line 421
    move-object/from16 v17, v3

    .line 422
    .line 423
    const/4 v3, 0x2

    .line 424
    move/from16 p1, v7

    .line 425
    .line 426
    new-array v7, v3, [F

    .line 427
    .line 428
    aput p1, v7, v16

    .line 429
    .line 430
    const/high16 v18, 0x3f800000    # 1.0f

    .line 431
    .line 432
    aput v18, v7, v21

    .line 433
    .line 434
    invoke-static {v5, v15, v7}, Landroid/animation/ObjectAnimator;->ofFloat(Ljava/lang/Object;Landroid/util/Property;[F)Landroid/animation/ObjectAnimator;

    .line 435
    .line 436
    .line 437
    move-result-object v7

    .line 438
    aput-object v7, v14, v16

    .line 439
    .line 440
    sget-object v7, Landroid/view/View;->SCALE_Y:Landroid/util/Property;

    .line 441
    .line 442
    new-array v15, v3, [F

    .line 443
    .line 444
    aput p1, v15, v16

    .line 445
    .line 446
    aput v18, v15, v21

    .line 447
    .line 448
    invoke-static {v5, v7, v15}, Landroid/animation/ObjectAnimator;->ofFloat(Ljava/lang/Object;Landroid/util/Property;[F)Landroid/animation/ObjectAnimator;

    .line 449
    .line 450
    .line 451
    move-result-object v5

    .line 452
    aput-object v5, v14, v21

    .line 453
    .line 454
    sget-object v5, Landroid/view/View;->SCALE_X:Landroid/util/Property;

    .line 455
    .line 456
    new-array v7, v3, [F

    .line 457
    .line 458
    aput v18, v7, v16

    .line 459
    .line 460
    aput v9, v7, v21

    .line 461
    .line 462
    invoke-static {v6, v5, v7}, Landroid/animation/ObjectAnimator;->ofFloat(Ljava/lang/Object;Landroid/util/Property;[F)Landroid/animation/ObjectAnimator;

    .line 463
    .line 464
    .line 465
    move-result-object v5

    .line 466
    aput-object v5, v14, v3

    .line 467
    .line 468
    sget-object v5, Landroid/view/View;->SCALE_Y:Landroid/util/Property;

    .line 469
    .line 470
    new-array v7, v3, [F

    .line 471
    .line 472
    aput v18, v7, v16

    .line 473
    .line 474
    aput v9, v7, v21

    .line 475
    .line 476
    invoke-static {v6, v5, v7}, Landroid/animation/ObjectAnimator;->ofFloat(Ljava/lang/Object;Landroid/util/Property;[F)Landroid/animation/ObjectAnimator;

    .line 477
    .line 478
    .line 479
    move-result-object v5

    .line 480
    aput-object v5, v14, v19

    .line 481
    .line 482
    invoke-virtual {v13, v14}, Landroid/animation/AnimatorSet;->playTogether([Landroid/animation/Animator;)V

    .line 483
    .line 484
    .line 485
    iget-object v5, v1, Lqmt;->d:Landroid/animation/TimeInterpolator;

    .line 486
    .line 487
    invoke-virtual {v13, v5}, Landroid/animation/AnimatorSet;->setInterpolator(Landroid/animation/TimeInterpolator;)V

    .line 488
    .line 489
    .line 490
    move/from16 v5, v21

    .line 491
    .line 492
    new-array v6, v5, [Landroid/animation/Animator;

    .line 493
    .line 494
    aput-object v13, v6, v16

    .line 495
    .line 496
    invoke-virtual {v2, v6}, Landroid/animation/AnimatorSet;->playTogether([Landroid/animation/Animator;)V

    .line 497
    .line 498
    .line 499
    iget-object v5, v1, Lqmt;->g:Lqmu;

    .line 500
    .line 501
    iget-object v6, v5, Lqmu;->h:Landroid/view/View;

    .line 502
    .line 503
    if-eqz v6, :cond_10

    .line 504
    .line 505
    iget-object v6, v12, Lqmc;->a:Landroid/graphics/Point;

    .line 506
    .line 507
    iget-object v7, v8, Lqmc;->a:Landroid/graphics/Point;

    .line 508
    .line 509
    iget-object v8, v5, Lqmu;->g:Lqmr;

    .line 510
    .line 511
    iget v9, v6, Landroid/graphics/Point;->x:I

    .line 512
    .line 513
    invoke-virtual {v8}, Lqmr;->b()I

    .line 514
    .line 515
    .line 516
    move-result v12

    .line 517
    mul-int/2addr v9, v12

    .line 518
    iget v12, v7, Landroid/graphics/Point;->x:I

    .line 519
    .line 520
    invoke-virtual {v8}, Lqmr;->b()I

    .line 521
    .line 522
    .line 523
    move-result v13

    .line 524
    mul-int/2addr v12, v13

    .line 525
    iget v6, v6, Landroid/graphics/Point;->y:I

    .line 526
    .line 527
    invoke-virtual {v8}, Lqmr;->a()I

    .line 528
    .line 529
    .line 530
    move-result v13

    .line 531
    mul-int/2addr v6, v13

    .line 532
    iget v7, v7, Landroid/graphics/Point;->y:I

    .line 533
    .line 534
    invoke-virtual {v8}, Lqmr;->a()I

    .line 535
    .line 536
    .line 537
    move-result v8

    .line 538
    mul-int/2addr v7, v8

    .line 539
    iget-object v14, v5, Lqmu;->h:Landroid/view/View;

    .line 540
    .line 541
    invoke-virtual {v14}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    .line 542
    .line 543
    .line 544
    move-result-object v8

    .line 545
    if-eqz v8, :cond_f

    .line 546
    .line 547
    iget v13, v8, Landroid/view/ViewGroup$LayoutParams;->width:I

    .line 548
    .line 549
    if-lez v13, :cond_f

    .line 550
    .line 551
    iget-object v8, v5, Lqmu;->g:Lqmr;

    .line 552
    .line 553
    invoke-virtual {v8}, Lqmr;->b()I

    .line 554
    .line 555
    .line 556
    move-result v19

    .line 557
    sub-int v19, v19, v13

    .line 558
    .line 559
    div-int/lit8 v19, v19, 0x2

    .line 560
    .line 561
    add-int v9, v9, v19

    .line 562
    .line 563
    add-int v12, v12, v19

    .line 564
    .line 565
    iget-object v13, v5, Lqmu;->h:Landroid/view/View;

    .line 566
    .line 567
    invoke-virtual {v13}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    .line 568
    .line 569
    .line 570
    move-result-object v13

    .line 571
    iget v13, v13, Landroid/view/ViewGroup$LayoutParams;->height:I

    .line 572
    .line 573
    if-lez v13, :cond_f

    .line 574
    .line 575
    invoke-virtual {v8}, Lqmr;->a()I

    .line 576
    .line 577
    .line 578
    move-result v19

    .line 579
    sub-int v19, v19, v13

    .line 580
    .line 581
    div-int/lit8 v19, v19, 0x2

    .line 582
    .line 583
    add-int v6, v6, v19

    .line 584
    .line 585
    add-int v7, v7, v19

    .line 586
    .line 587
    :cond_f
    new-instance v8, Landroid/animation/AnimatorSet;

    .line 588
    .line 589
    invoke-direct {v8}, Landroid/animation/AnimatorSet;-><init>()V

    .line 590
    .line 591
    .line 592
    new-array v13, v3, [Landroid/animation/Animator;

    .line 593
    .line 594
    iget-object v14, v5, Lqmu;->h:Landroid/view/View;

    .line 595
    .line 596
    sget-object v15, Landroid/view/View;->TRANSLATION_X:Landroid/util/Property;

    .line 597
    .line 598
    int-to-float v9, v9

    .line 599
    int-to-float v12, v12

    .line 600
    move/from16 v18, v9

    .line 601
    .line 602
    new-array v9, v3, [F

    .line 603
    .line 604
    aput v18, v9, v16

    .line 605
    .line 606
    const/4 v3, 0x1

    .line 607
    aput v12, v9, v3

    .line 608
    .line 609
    invoke-static {v14, v15, v9}, Landroid/animation/ObjectAnimator;->ofFloat(Ljava/lang/Object;Landroid/util/Property;[F)Landroid/animation/ObjectAnimator;

    .line 610
    .line 611
    .line 612
    move-result-object v9

    .line 613
    aput-object v9, v13, v16

    .line 614
    .line 615
    iget-object v5, v5, Lqmu;->h:Landroid/view/View;

    .line 616
    .line 617
    sget-object v9, Landroid/view/View;->TRANSLATION_Y:Landroid/util/Property;

    .line 618
    .line 619
    int-to-float v6, v6

    .line 620
    int-to-float v7, v7

    .line 621
    const/4 v12, 0x2

    .line 622
    new-array v12, v12, [F

    .line 623
    .line 624
    aput v6, v12, v16

    .line 625
    .line 626
    aput v7, v12, v3

    .line 627
    .line 628
    invoke-static {v5, v9, v12}, Landroid/animation/ObjectAnimator;->ofFloat(Ljava/lang/Object;Landroid/util/Property;[F)Landroid/animation/ObjectAnimator;

    .line 629
    .line 630
    .line 631
    move-result-object v5

    .line 632
    aput-object v5, v13, v3

    .line 633
    .line 634
    invoke-virtual {v8, v13}, Landroid/animation/AnimatorSet;->playTogether([Landroid/animation/Animator;)V

    .line 635
    .line 636
    .line 637
    iget-object v5, v1, Lqmt;->e:Landroid/animation/TimeInterpolator;

    .line 638
    .line 639
    invoke-virtual {v8, v5}, Landroid/animation/AnimatorSet;->setInterpolator(Landroid/animation/TimeInterpolator;)V

    .line 640
    .line 641
    .line 642
    new-array v3, v3, [Landroid/animation/Animator;

    .line 643
    .line 644
    aput-object v8, v3, v16

    .line 645
    .line 646
    invoke-virtual {v2, v3}, Landroid/animation/AnimatorSet;->playTogether([Landroid/animation/Animator;)V

    .line 647
    .line 648
    .line 649
    :cond_10
    const-wide/16 v5, 0x12c

    .line 650
    .line 651
    invoke-virtual {v2, v5, v6}, Landroid/animation/AnimatorSet;->setDuration(J)Landroid/animation/AnimatorSet;

    .line 652
    .line 653
    .line 654
    iput-object v2, v1, Lqmt;->c:Landroid/animation/AnimatorSet;

    .line 655
    .line 656
    iget-object v2, v1, Lqmt;->f:Landroid/animation/AnimatorListenerAdapter;

    .line 657
    .line 658
    if-nez v2, :cond_11

    .line 659
    .line 660
    new-instance v2, Lqms;

    .line 661
    .line 662
    invoke-direct {v2, v1}, Lqms;-><init>(Lqmt;)V

    .line 663
    .line 664
    .line 665
    iput-object v2, v1, Lqmt;->f:Landroid/animation/AnimatorListenerAdapter;

    .line 666
    .line 667
    :cond_11
    iget-object v2, v1, Lqmt;->c:Landroid/animation/AnimatorSet;

    .line 668
    .line 669
    iget-object v3, v1, Lqmt;->f:Landroid/animation/AnimatorListenerAdapter;

    .line 670
    .line 671
    invoke-virtual {v2, v3}, Landroid/animation/AnimatorSet;->addListener(Landroid/animation/Animator$AnimatorListener;)V

    .line 672
    .line 673
    .line 674
    iget-object v2, v1, Lqmt;->c:Landroid/animation/AnimatorSet;

    .line 675
    .line 676
    invoke-virtual {v2}, Landroid/animation/AnimatorSet;->start()V

    .line 677
    .line 678
    .line 679
    :goto_7
    iget-object v2, v1, Lqmt;->c:Landroid/animation/AnimatorSet;

    .line 680
    .line 681
    if-eqz v2, :cond_14

    .line 682
    .line 683
    invoke-virtual {v2}, Landroid/animation/AnimatorSet;->isRunning()Z

    .line 684
    .line 685
    .line 686
    move-result v2

    .line 687
    if-eqz v2, :cond_14

    .line 688
    .line 689
    iget-object v2, v1, Lqmt;->c:Landroid/animation/AnimatorSet;

    .line 690
    .line 691
    invoke-virtual {v2}, Landroid/animation/AnimatorSet;->isPaused()Z

    .line 692
    .line 693
    .line 694
    move-result v2

    .line 695
    if-nez v2, :cond_12

    .line 696
    .line 697
    iget-object v2, v1, Lqmt;->c:Landroid/animation/AnimatorSet;

    .line 698
    .line 699
    invoke-virtual {v2}, Landroid/animation/AnimatorSet;->pause()V

    .line 700
    .line 701
    .line 702
    :cond_12
    iget-object v2, v1, Lqmt;->c:Landroid/animation/AnimatorSet;

    .line 703
    .line 704
    invoke-virtual {v2}, Landroid/animation/AnimatorSet;->getDuration()J

    .line 705
    .line 706
    .line 707
    move-result-wide v2

    .line 708
    long-to-double v2, v2

    .line 709
    mul-double/2addr v10, v2

    .line 710
    iget-object v1, v1, Lqmt;->c:Landroid/animation/AnimatorSet;

    .line 711
    .line 712
    double-to-long v2, v10

    .line 713
    invoke-static {v1, v2, v3}, Lk$$ExternalSyntheticApiModelOutline1;->m(Landroid/animation/AnimatorSet;J)V

    .line 714
    .line 715
    .line 716
    goto :goto_8

    .line 717
    :cond_13
    move-object/from16 v17, v3

    .line 718
    .line 719
    invoke-virtual {v14}, Lqme;->b()V

    .line 720
    .line 721
    .line 722
    :cond_14
    :goto_8
    invoke-static/range {v17 .. v17}, Lmgw;->K(Lqmd;)Lqmc;

    .line 723
    .line 724
    .line 725
    move-result-object v1

    .line 726
    if-eq v1, v4, :cond_17

    .line 727
    .line 728
    if-nez v1, :cond_15

    .line 729
    .line 730
    invoke-virtual {v0}, Lqml;->b()V

    .line 731
    .line 732
    .line 733
    goto :goto_9

    .line 734
    :cond_15
    iget-object v1, v1, Lqmc;->c:Landroid/view/View;

    .line 735
    .line 736
    invoke-virtual {v0, v1}, Lqml;->h(Landroid/view/View;)V

    .line 737
    .line 738
    .line 739
    :goto_9
    iget-object v1, v0, Lqmu;->i:Lqmt;

    .line 740
    .line 741
    invoke-virtual {v1}, Lqmt;->b()Z

    .line 742
    .line 743
    .line 744
    move-result v2

    .line 745
    if-eqz v2, :cond_16

    .line 746
    .line 747
    iget-object v1, v1, Lqmt;->b:Lqmc;

    .line 748
    .line 749
    if-eq v4, v1, :cond_17

    .line 750
    .line 751
    invoke-virtual {v0, v4}, Lqmu;->m(Lqmc;)V

    .line 752
    .line 753
    .line 754
    goto :goto_a

    .line 755
    :cond_16
    invoke-virtual {v0, v4}, Lqmu;->m(Lqmc;)V

    .line 756
    .line 757
    .line 758
    invoke-virtual {v0}, Lqmu;->n()V

    .line 759
    .line 760
    .line 761
    :cond_17
    :goto_a
    return-void
.end method

.method public final m(Lqmc;)V
    .locals 3

    .line 1
    if-eqz p1, :cond_1

    .line 2
    .line 3
    iget-boolean v0, p0, Lqmu;->a:Z

    .line 4
    .line 5
    iget-object v1, p1, Lqmc;->d:Landroid/view/View;

    .line 6
    .line 7
    const/high16 v2, 0x3f800000    # 1.0f

    .line 8
    .line 9
    if-eqz v0, :cond_0

    .line 10
    .line 11
    iget-object p1, p1, Lqmc;->a:Landroid/graphics/Point;

    .line 12
    .line 13
    iget-object p0, p0, Lqmu;->g:Lqmr;

    .line 14
    .line 15
    iget-object p0, p0, Lqmr;->c:Landroid/graphics/Point;

    .line 16
    .line 17
    invoke-virtual {p1, p0}, Landroid/graphics/Point;->equals(Ljava/lang/Object;)Z

    .line 18
    .line 19
    .line 20
    move-result p0

    .line 21
    if-eqz p0, :cond_0

    .line 22
    .line 23
    invoke-static {v1}, Lqmu;->l(Landroid/view/View;)F

    .line 24
    .line 25
    .line 26
    move-result v2

    .line 27
    :cond_0
    invoke-virtual {v1, v2}, Landroid/view/View;->setScaleX(F)V

    .line 28
    .line 29
    .line 30
    invoke-virtual {v1, v2}, Landroid/view/View;->setScaleY(F)V

    .line 31
    .line 32
    .line 33
    :cond_1
    return-void
.end method

.method public final n()V
    .locals 1

    .line 1
    iget-object v0, p0, Lqmu;->g:Lqmr;

    .line 2
    .line 3
    invoke-static {v0}, Lmgw;->K(Lqmd;)Lqmc;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    if-eqz v0, :cond_0

    .line 8
    .line 9
    invoke-virtual {p0, v0}, Lqmu;->m(Lqmc;)V

    .line 10
    .line 11
    .line 12
    invoke-direct {p0, v0}, Lqmu;->o(Lqmc;)V

    .line 13
    .line 14
    .line 15
    :cond_0
    return-void
.end method
