#!/bin/bash
# Script pour pousser les modifications vers GitHub et déclencher la compilation AppVeyor

echo "🚀 M-REZO - Push vers GitHub"
echo ""

# Vérifier si on est dans le bon dossier
if [ ! -f "appveyor.yml" ]; then
    echo "❌ Erreur: Exécutez ce script depuis le dossier racine du projet M-REZO"
    exit 1
fi

# Status git
echo "📊 État actuel du dépôt:"
git status --short
echo ""

# Demander confirmation
read -p "Voulez-vous commiter ces changements? (o/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Oo]$ ]]; then
    echo "❌ Annulé"
    exit 1
fi

# Demander le message de commit
echo "✏️  Entrez le message de commit:"
read commit_message

if [ -z "$commit_message" ]; then
    echo "❌ Message de commit vide, annulé"
    exit 1
fi

# Add, commit, push
echo ""
echo "📦 Ajout des fichiers..."
git add .

echo "💾 Création du commit..."
git commit -m "$commit_message

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"

echo "⬆️  Push vers GitHub..."
git push

echo ""
echo "✅ Terminé! La compilation AppVeyor va démarrer automatiquement."
echo "🔗 Vérifiez: https://ci.appveyor.com/project/VOTRE-USERNAME/m-rezo"
