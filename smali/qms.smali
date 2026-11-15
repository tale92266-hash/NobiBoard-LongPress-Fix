.class final Lqms;
.super Landroid/animation/AnimatorListenerAdapter;
.source "PG"


# instance fields
.field final synthetic a:Lqmt;


# direct methods
.method public constructor <init>(Lqmt;)V
    .locals 0

    .line 1
    invoke-static {p1}, Lj$/util/Objects;->requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;

    .line 2
    .line 3
    .line 4
    iput-object p1, p0, Lqms;->a:Lqmt;

    .line 5
    .line 6
    invoke-direct {p0}, Landroid/animation/AnimatorListenerAdapter;-><init>()V

    .line 7
    .line 8
    .line 9
    return-void
.end method


# virtual methods
.method public final onAnimationEnd(Landroid/animation/Animator;)V
    .locals 1

    .line 1
    invoke-super {p0, p1}, Landroid/animation/AnimatorListenerAdapter;->onAnimationEnd(Landroid/animation/Animator;)V

    .line 2
    .line 3
    .line 4
    iget-object p0, p0, Lqms;->a:Lqmt;

    .line 5
    .line 6
    iget-object p1, p0, Lqmt;->g:Lqmu;

    .line 7
    .line 8
    iget-object v0, p0, Lqmt;->a:Lqmc;

    .line 9
    .line 10
    invoke-virtual {p1, v0}, Lqmu;->m(Lqmc;)V

    .line 11
    .line 12
    .line 13
    iget-object v0, p0, Lqmt;->b:Lqmc;

    .line 14
    .line 15
    invoke-virtual {p1, v0}, Lqmu;->m(Lqmc;)V

    .line 16
    .line 17
    .line 18
    invoke-virtual {p1}, Lqmu;->n()V

    .line 19
    .line 20
    .line 21
    invoke-virtual {p0}, Lqmt;->a()V

    .line 22
    .line 23
    .line 24
    return-void
.end method
