.class public final Lqmr;
.super Ljava/lang/Object;
.source "PG"

# interfaces
.implements Lqmd;


# instance fields
.field public final b:Landroid/graphics/Point;

.field public final c:Landroid/graphics/Point;

.field public final d:Ljava/util/List;

.field final synthetic e:Lqmu;


# direct methods
.method public constructor <init>(Lqmu;)V
    .locals 1

    .line 1
    invoke-static {p1}, Lj$/util/Objects;->requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;

    .line 2
    .line 3
    .line 4
    iput-object p1, p0, Lqmr;->e:Lqmu;

    .line 5
    .line 6
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 7
    .line 8
    .line 9
    new-instance p1, Landroid/graphics/Point;

    .line 10
    .line 11
    sget-object v0, Lqmr;->a:Landroid/graphics/Point;

    .line 12
    .line 13
    invoke-direct {p1, v0}, Landroid/graphics/Point;-><init>(Landroid/graphics/Point;)V

    .line 14
    .line 15
    .line 16
    iput-object p1, p0, Lqmr;->b:Landroid/graphics/Point;

    .line 17
    .line 18
    new-instance p1, Landroid/graphics/Point;

    .line 19
    .line 20
    invoke-direct {p1, v0}, Landroid/graphics/Point;-><init>(Landroid/graphics/Point;)V

    .line 21
    .line 22
    .line 23
    iput-object p1, p0, Lqmr;->c:Landroid/graphics/Point;

    .line 24
    .line 25
    new-instance p1, Ljava/util/ArrayList;

    .line 26
    .line 27
    invoke-direct {p1}, Ljava/util/ArrayList;-><init>()V

    .line 28
    .line 29
    .line 30
    iput-object p1, p0, Lqmr;->d:Ljava/util/List;

    .line 31
    .line 32
    return-void
.end method


# virtual methods
.method public final a()I
    .locals 0

    .line 1
    iget-object p0, p0, Lqmr;->e:Lqmu;

    .line 2
    .line 3
    iget-object p0, p0, Lqmu;->f:Lqmo;

    .line 4
    .line 5
    iget-object p0, p0, Lqmo;->d:Lqmn;

    .line 6
    .line 7
    iget p0, p0, Lqmn;->b:I

    .line 8
    .line 9
    return p0
.end method

.method public final b()I
    .locals 0

    .line 1
    iget-object p0, p0, Lqmr;->e:Lqmu;

    .line 2
    .line 3
    iget-object p0, p0, Lqmu;->f:Lqmo;

    .line 4
    .line 5
    iget-object p0, p0, Lqmo;->d:Lqmn;

    .line 6
    .line 7
    iget p0, p0, Lqmn;->a:I

    .line 8
    .line 9
    return p0
.end method

.method public final c()I
    .locals 0

    .line 1
    iget-object p0, p0, Lqmr;->e:Lqmu;

    .line 2
    .line 3
    iget-object p0, p0, Lqmu;->f:Lqmo;

    .line 4
    .line 5
    iget-object p0, p0, Lqmo;->d:Lqmn;

    .line 6
    .line 7
    iget p0, p0, Lqmn;->e:I

    .line 8
    .line 9
    return p0
.end method

.method public final d()I
    .locals 0

    .line 1
    iget-object p0, p0, Lqmr;->e:Lqmu;

    .line 2
    .line 3
    iget-object p0, p0, Lqmu;->f:Lqmo;

    .line 4
    .line 5
    iget-object p0, p0, Lqmo;->d:Lqmn;

    .line 6
    .line 7
    iget p0, p0, Lqmn;->d:I

    .line 8
    .line 9
    return p0
.end method
