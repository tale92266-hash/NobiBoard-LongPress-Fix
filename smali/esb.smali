.class public Lesb;
.super Lesf;
.source "PG"

# interfaces
.implements Laawk;


# instance fields
.field private g:Z

.field private final h:Laavt;


# direct methods
.method public constructor <init>()V
    .locals 2

    .line 1
    invoke-direct {p0}, Lesf;-><init>()V

    .line 2
    .line 3
    .line 4
    const/4 v0, 0x0

    .line 5
    iput-boolean v0, p0, Lesb;->g:Z

    .line 6
    .line 7
    new-instance v0, Laavt;

    .line 8
    .line 9
    new-instance v1, Laczj;

    .line 10
    .line 11
    invoke-direct {v1, p0}, Laczj;-><init>(Lesb;)V

    .line 12
    .line 13
    .line 14
    invoke-direct {v0, v1}, Laavt;-><init>(Laczj;)V

    .line 15
    .line 16
    .line 17
    iput-object v0, p0, Lesb;->h:Laavt;

    .line 18
    .line 19
    return-void
.end method


# virtual methods
.method public final synthetic a()Laawj;
    .locals 0

    .line 1
    iget-object p0, p0, Lesb;->h:Laavt;

    .line 2
    .line 3
    return-object p0
.end method

.method public final b()Ljava/lang/Object;
    .locals 0

    .line 1
    iget-object p0, p0, Lesb;->h:Laavt;

    .line 2
    .line 3
    invoke-virtual {p0}, Laavt;->b()Ljava/lang/Object;

    .line 4
    .line 5
    .line 6
    move-result-object p0

    .line 7
    return-object p0
.end method

.method public final onCreate()V
    .locals 1

    .line 1
    sput-object p0, Lpud;->appCtx:Landroid/content/Context;

    .line 2
    .line 3
    iget-boolean v0, p0, Lesb;->g:Z

    .line 4
    .line 5
    if-nez v0, :cond_0

    .line 6
    .line 7
    const/4 v0, 0x1

    .line 8
    iput-boolean v0, p0, Lesb;->g:Z

    .line 9
    .line 10
    invoke-virtual {p0}, Lesb;->b()Ljava/lang/Object;

    .line 11
    .line 12
    .line 13
    move-object v0, p0

    .line 14
    check-cast v0, Lcom/google/android/apps/inputmethod/latin/HiltLatinApp;

    .line 15
    .line 16
    :cond_0
    invoke-super {p0}, Lesf;->onCreate()V

    .line 17
    .line 18
    .line 19
    return-void
.end method
