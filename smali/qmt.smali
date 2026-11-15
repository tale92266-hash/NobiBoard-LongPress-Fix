.class final Lqmt;
.super Ljava/lang/Object;
.source "PG"


# instance fields
.field public a:Lqmc;

.field public b:Lqmc;

.field public c:Landroid/animation/AnimatorSet;

.field public final d:Landroid/animation/TimeInterpolator;

.field public final e:Landroid/animation/TimeInterpolator;

.field public f:Landroid/animation/AnimatorListenerAdapter;

.field final synthetic g:Lqmu;


# direct methods
.method public constructor <init>(Lqmu;)V
    .locals 4

    .line 1
    invoke-static {p1}, Lj$/util/Objects;->requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;

    .line 2
    .line 3
    .line 4
    iput-object p1, p0, Lqmt;->g:Lqmu;

    .line 5
    .line 6
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 7
    .line 8
    .line 9
    new-instance p1, Landroid/view/animation/PathInterpolator;

    .line 10
    .line 11
    const v0, 0x3ecccccd    # 0.4f

    .line 12
    .line 13
    .line 14
    const/4 v1, 0x0

    .line 15
    const v2, 0x3f19999a    # 0.6f

    .line 16
    .line 17
    .line 18
    const/high16 v3, 0x3f800000    # 1.0f

    .line 19
    .line 20
    invoke-direct {p1, v0, v1, v2, v3}, Landroid/view/animation/PathInterpolator;-><init>(FFFF)V

    .line 21
    .line 22
    .line 23
    iput-object p1, p0, Lqmt;->d:Landroid/animation/TimeInterpolator;

    .line 24
    .line 25
    new-instance p1, Landroid/view/animation/PathInterpolator;

    .line 26
    .line 27
    const v0, 0x3e4ccccd    # 0.2f

    .line 28
    .line 29
    .line 30
    invoke-direct {p1, v3, v1, v0, v3}, Landroid/view/animation/PathInterpolator;-><init>(FFFF)V

    .line 31
    .line 32
    .line 33
    iput-object p1, p0, Lqmt;->e:Landroid/animation/TimeInterpolator;

    .line 34
    .line 35
    return-void
.end method


# virtual methods
.method public final a()V
    .locals 1

    .line 1
    const/4 v0, 0x0

    .line 2
    iput-object v0, p0, Lqmt;->a:Lqmc;

    .line 3
    .line 4
    iput-object v0, p0, Lqmt;->b:Lqmc;

    .line 5
    .line 6
    iput-object v0, p0, Lqmt;->c:Landroid/animation/AnimatorSet;

    .line 7
    .line 8
    return-void
.end method

.method public final b()Z
    .locals 0

    .line 1
    iget-object p0, p0, Lqmt;->c:Landroid/animation/AnimatorSet;

    .line 2
    .line 3
    if-eqz p0, :cond_0

    .line 4
    .line 5
    invoke-virtual {p0}, Landroid/animation/AnimatorSet;->isRunning()Z

    .line 6
    .line 7
    .line 8
    move-result p0

    .line 9
    if-eqz p0, :cond_0

    .line 10
    .line 11
    const/4 p0, 0x1

    .line 12
    goto :goto_0

    .line 13
    :cond_0
    const/4 p0, 0x0

    .line 14
    :goto_0
    return p0
.end method
